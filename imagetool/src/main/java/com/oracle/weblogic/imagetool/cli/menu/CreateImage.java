// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
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
public class CreateImage extends ImageOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateImage.class);

    public CreateImage() {
    }

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();

        String tmpDir = null;

        try {

            CommandResponse result = super.call();
            if (result.getStatus() != 0) {
                return result;
            }

            tmpDir = getTempDirectory();

            if (!Utils.validatePatchIds(patches, false)) {
                return new CommandResponse(-1, "Patch ID validation failed");
            }

            if (fromImage != null && !fromImage.isEmpty()) {
                logger.finer("IMG-0002", fromImage);
                dockerfileOptions.setBaseImage(fromImage);

                Utils.copyResourceAsFile("/probe-env/test-create-env.sh",
                        tmpDir + File.separator + "test-env.sh", true);

                Properties baseImageProperties = Utils.getBaseImageProperties(fromImage, tmpDir);

                boolean ohAlreadyExists = baseImageProperties.getProperty("WLS_VERSION", null) != null;

                String existingJavaHome = baseImageProperties.getProperty("JAVA_HOME", null);
                if (existingJavaHome != null) {
                    dockerfileOptions.disableJavaInstall(existingJavaHome);
                    logger.info("IMG-0000", existingJavaHome);
                }

                if (ohAlreadyExists) {
                    return new CommandResponse(-1,
                            "Oracle Home exists at location:" + baseImageProperties.getProperty("ORACLE_HOME"));
                }

                String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
                if (!Utils.isEmptyString(pkgMgr)) {
                    dockerfileOptions.setPackageInstaller(pkgMgr);
                }
            } else {
                dockerfileOptions.setPackageInstaller(Constants.YUM);
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            // this handles wls, jdk and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWdtArgsIfRequired(tmpDir));

            // If patching, patch OPatch first
            if (applyingPatches()) {
                addOPatch1394ToImage(tmpDir, opatchBugNumber);
            }

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(null));

            // Copy wls response file to tmpDir
            copyResponseFilesToDir(tmpDir);
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

    /**
     * Builds a list of {@link InstallerFile} objects based on user input which are processed.
     * to download the required install artifacts
     *
     * @return list of InstallerFile
     * @throws Exception in case of error
     */
    protected List<InstallerFile> gatherRequiredInstallers() throws Exception {
        logger.entering();
        List<InstallerFile> retVal = super.gatherRequiredInstallers();
        logger.finer("IMG-0001", getInstallerType(), getInstallerVersion());
        retVal.add(new InstallerFile(useCache, InstallerType.fromValue(getInstallerType().toString()),
            getInstallerVersion(), userId, password));
        if (dockerfileOptions.installJava()) {
            logger.finer("IMG-0001", InstallerType.JDK, jdkVersion);
            retVal.add(new InstallerFile(useCache, InstallerType.JDK, jdkVersion, userId, password));
        }
        logger.exiting(retVal.size());
        return retVal;
    }


    /**
     * Copies response files required for wls install to the tmp directory which provides docker build context.
     *
     * @param dirPath directory to copy to
     * @throws IOException in case of error
     */
    private void copyResponseFilesToDir(String dirPath) throws IOException {
        if (installerResponseFile != null && Files.isRegularFile(Paths.get(installerResponseFile))) {
            logger.fine("IMG-0005", installerResponseFile);
            Path target = Paths.get(dirPath, "wls.rsp");
            Files.copy(Paths.get(installerResponseFile), target);
        } else {
            final String responseFile = "/response-files/wls.rsp";
            logger.fine("IMG-0005", responseFile);
            Utils.copyResourceAsFile(responseFile, dirPath, false);
        }

        Utils.copyResourceAsFile("/response-files/oraInst.loc", dirPath, false);
    }

    @Override
    public WLSInstallerType getInstallerType() {
        if (installerType == null) {
            return getWdtDomainType().installerType();
        }
        return installerType;
    }

    @Override
    public String getInstallerVersion() {
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
}
