// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.FmwInstallerType;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.installers.MiddlewareInstall;
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

        try {
            init(buildId);

            tmpDir = getTempDirectory();

            if (!Utils.validatePatchIds(patches, false)) {
                return new CommandResponse(-1, "Patch ID validation failed");
            }

            WLSInstallHelper.copyOptionsFromImage(fromImage, dockerfileOptions, tmpDir);

            List<String> cmdBuilder = getInitialBuildCmd();

            if (dockerfileOptions.installJava()) {
                CachedFile jdk = new CachedFile(InstallerType.JDK, jdkVersion);
                Path installerPath = jdk.copyFile(cacheStore, tmpDir);
                dockerfileOptions.setJavaInstaller(installerPath.getFileName().toString());
            }

            MiddlewareInstall install = MiddlewareInstall.getInstall(this.installerType, installerVersion);
            install.copyFiles(cacheStore, tmpDir);
            dockerfileOptions.setMiddlewareInstall(install);

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(wdtOptions.handleWdtArgsIfRequired(dockerfileOptions, tmpDir, getInstallerType()));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(null));

            // If patching, patch OPatch first
            if (applyingPatches()) {
                installOpatchInstaller(tmpDir, opatchBugNumber);
            }

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
            } else {
                Utils.copyResourceAsFile("/response-files/oraInst.loc", tmpDir, false);
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

    private List<CachedFile> gatherRequiredInstallers() throws Exception {
        logger.entering();
        List<CachedFile> retVal = wdtOptions.gatherWdtRequiredInstallers();
        return WLSInstallHelper.getBasicInstallers(retVal, getInstallerType().toString(),
            getInstallerVersion(), jdkVersion, dockerfileOptions);
    }


    @Override
    FmwInstallerType getInstallerType() {
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
    private FmwInstallerType installerType = FmwInstallerType.WLS;

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
