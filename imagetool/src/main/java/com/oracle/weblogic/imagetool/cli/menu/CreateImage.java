// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "create",
        description = "Build WebLogic docker image",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class CreateImage extends ImageBuildWithWDTOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateImage.class);
    String password;

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();
        String tmpDir = null;
        String buildId =  UUID.randomUUID().toString();

        try {
            password = init(buildId);

            tmpDir = getTempDirectory();

            OptionsHelper optionsHelper = new OptionsHelper(this,
                dockerfileOptions, getInstallerType(), installerVersion, password, tmpDir);

            //OptionsHelper optionsHelper = new OptionsHelper(latestPSU, patches, userId, password, useCache,
            //    cacheStore, dockerfileOptions, getInstallerType(), installerVersion, tmpDir);

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
            cmdBuilder.addAll(optionsHelper.handleInstallerFiles(tmpDir, gatherRequiredInstallers()));

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWdtArgsIfRequired(tmpDir, getInstallerType()));

            // resolve required patches
            cmdBuilder.addAll(optionsHelper.handlePatchFiles(null));

            // If patching, patch OPatch first
            if (optionsHelper.applyingPatches()) {
                optionsHelper.addOPatch1394ToImage(tmpDir, opatchBugNumber);
            }

            // Copy wls response file to tmpDir
            WLSInstallHelper.copyResponseFilesToDir(tmpDir, installerResponseFile);
            Utils.setOracleHome(installerResponseFile, dockerfileOptions);

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(tmpDir + File.separator + "Dockerfile",
                "Create_Image.mustache", dockerfileOptions, dryRun);

            // add directory to pass the context
            cmdBuilder.add(tmpDir);
            runDockerCommand(dockerfile, cmdBuilder);
        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            if (cleanup) {
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
        List<InstallerFile> retVal = gatherWDTRequiredInstallers();
        return WLSInstallHelper.getBasicInstallers(retVal, getInstallerType().toString(),
            installerVersion, jdkVersion, dockerfileOptions, userId, password, useCache);
    }


    public WLSInstallerType getInstallerType() {
        if (installerType == null) {
            return getWdtDomainType().installerType();
        }
        return installerType;
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
}
