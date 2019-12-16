// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.InstallerFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "create",
        description = "Build WebLogic docker image",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class CreateImage extends CommonOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateImage.class);

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();
        String tmpDir = null;
        String buildId =  UUID.randomUUID().toString();

        if (wdtOptions == null)
            System.out.println("WDT opts is null");

        try {
            init(buildId);

            tmpDir = getTempDirectory();

            if (!Utils.validatePatchIds(patches, false)) {
                return new CommandResponse(-1, "Patch ID validation failed");
            }

            boolean rc = WLSInstallHelper.setFromImage(fromImage, dockerfileOptions, tmpDir);
            if (!rc) {
                Properties baseImageProperties = Utils.getBaseImageProperties(fromImage, tmpDir);
                return new CommandResponse(-1,
                    "Oracle Home exists at location:" + baseImageProperties.getProperty("ORACLE_HOME"));
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            // this handles wls, jdk and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir, gatherRequiredInstallers()));

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(wdtOptions.handleWdtArgsIfRequired(dockerfileOptions, tmpDir, getInstallerType()));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(null));

            // If patching, patch OPatch first
            if (applyingPatches()) {
                installOpatchInstaller(tmpDir, opatchBugNumber);
            }

            // Copy wls response file to tmpDir
            WLSInstallHelper.copyResponseFilesToDir(tmpDir, installerResponseFile);

            Utils.setOracleHome(installerResponseFile, dockerfileOptions);

            // Set where the user want to install the inventor oraInst.loc file

            if (inventoryPointerInstallLoc != null) {
                dockerfileOptions.setInventoryPointerFileSet(true);
                dockerfileOptions.setInvLoc(inventoryPointerInstallLoc);
            }

            // Set the inventory location, so that it will be copied
            if (inventoryPointerFile != null) {
                Utils.setInventoryLocation(inventoryPointerFile, dockerfileOptions);
                Utils.copyLocalFile(inventoryPointerFile, tmpDir + "/oraInst.loc", false);
            }

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(tmpDir + File.separator + "Dockerfile",
                "Create_Image.mustache", dockerfileOptions, dryRun);

            // add directory to pass the context
            cmdBuilder.add(tmpDir);
            runDockerCommand(dockerfile, cmdBuilder);
        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            if (!skipcleanup) {
                Utils.deleteFilesRecursively(tmpDir);
                Utils.removeIntermediateDockerImages(buildId);
            }
        }
        Instant endTime = Instant.now();
        logger.exiting();
        return new CommandResponse(0, "build successful in "
                + Duration.between(startTime, endTime).getSeconds() + "s. image tag: " + imageTag);
    }

    private List<InstallerFile> gatherRequiredInstallers() throws Exception {
        logger.entering();
        List<InstallerFile> retVal = wdtOptions.gatherWdtRequiredInstallers();
        return WLSInstallHelper.getBasicInstallers(retVal, getInstallerType().toString(),
            getInstallerVersion(), jdkVersion, dockerfileOptions);
    }


    @Override
    WLSInstallerType getInstallerType() {
        if (installerType == null) {
            return wdtOptions.getWdtDomainType().installerType();
        }
        return installerType;
    }

    @Override
    String getInstallerVersion() {
        return installerVersion;
    }

    @Option(
            names = {"--type"},
            description = "Installer type. Default: WLS. Supported values: ${COMPLETION-CANDIDATES}"
    )
    private WLSInstallerType installerType;

    @Option(
            names = {"--version"},
            description = "Installer version. Default: ${DEFAULT-VALUE}",
            required = true,
            defaultValue = Constants.DEFAULT_WLS_VERSION
    )
    private String installerVersion;

    @Option(
            names = {"--jdkVersion"},
            description = "Version of server jdk to install. Default: ${DEFAULT-VALUE}",
            required = true,
            defaultValue = Constants.DEFAULT_JDK_VERSION
    )
    private String jdkVersion;

    @Option(
            names = {"--fromImage"},
            description = "Docker image to use as base image."
    )
    private String fromImage;

    @Option(
            names = {"--installerResponseFile"},
            description = "path to a response file. Override the default responses for the Oracle installer"
    )
    private String installerResponseFile;

    @Option(
        names = {"--inventoryPointerFile"},
        description = "path to a user provided inventory pointer file as input"
    )
    private String inventoryPointerFile;

    @Option(
        names = {"--inventoryPointerInstallLoc"},
        description = "path to where the inventory pointer file (oraInst.loc) should be stored in the image"
    )
    private String inventoryPointerInstallLoc;

    @ArgGroup(exclusive = false, heading = "WDT Options%n")
    private WdtOptions wdtOptions = new WdtOptions();
}
