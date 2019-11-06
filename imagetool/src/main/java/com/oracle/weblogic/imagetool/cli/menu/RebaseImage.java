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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.InstallerFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "rebase",
    description = "Copy domain from one image to another",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class RebaseImage extends ImageBuildOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(RebaseImage.class);
    String tempDirectory;
    String password = null;

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();

        String tmpDir = null;
        String buildId = UUID.randomUUID().toString();
        dockerfileOptions = new DockerfileOptions(buildId);
        String oldOracleHome;
        String oldJavaHome;
        String newOracleHome = null;
        String newJavaHome = null;
        String domainHome;

        try {

            tmpDir = getTempDirectory();

            if (sourceImage != null && !sourceImage.isEmpty()) {
                logger.finer("IMG-0002", sourceImage);
                dockerfileOptions.setSourceImage(sourceImage);

                // Get source image
                Utils.copyResourceAsFile("/probe-env/test-update-env.sh",
                    tmpDir + File.separator + "test-env.sh", true);

                Properties baseImageProperties = Utils.getBaseImageProperties(sourceImage, tmpDir);

                oldOracleHome = baseImageProperties.getProperty("ORACLE_HOME", null);
                oldJavaHome = baseImageProperties.getProperty("JAVA_PATH", null);
                domainHome = baseImageProperties.getProperty("DOMAIN_HOME", null);

            } else {
                return new CommandResponse(-1, "Source Image not set");
            }


            if (targetImage != null && !targetImage.isEmpty()) {
                logger.finer("IMG-0002", targetImage);
                dockerfileOptions.setTargetImage(targetImage);
                dockerfileOptions.setRebaseToTarget(true);
                // Get source image
                Utils.copyResourceAsFile("/probe-env/test-update-env.sh",
                    tmpDir + File.separator + "test-env.sh", true);

                Properties baseImageProperties = Utils.getBaseImageProperties(targetImage, tmpDir);

                newOracleHome = baseImageProperties.getProperty("ORACLE_HOME", null);
                newJavaHome = baseImageProperties.getProperty("JAVA_PATH", null);

            } else {
                dockerfileOptions.setRebaseToNew(true);
                //return new CommandResponse(-1, "Target Image not set");
            }

            if (newJavaHome != null && !newJavaHome.equals(oldJavaHome)) {
                return new CommandResponse(-1, "Java Home changed in new image");
            }

            if (!newOracleHome.equals(oldOracleHome)) {
                return new CommandResponse(-1, "Oracle Home changed in new image");
            }

            if (domainHome != null && !domainHome.isEmpty()) {
                dockerfileOptions.setDomainHome(domainHome);
            } else {
                return new CommandResponse(-1, "No Domain Home in source imange");
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            if (dockerfileOptions.isRebaseToNew()) {

                handleProxyUrls();
                password = handlePasswordOptions();

                OptionsHelper optionsHelper = new OptionsHelper(latestPSU, patches, userId, password, useCache,
                    cacheStore, dockerfileOptions, getInstallerType(), getInstallerVersion(), tmpDir);

                // this handles wls, jdk and wdt install files.
                cmdBuilder.addAll(optionsHelper.handleInstallerFiles(tmpDir, gatherRequiredInstallers()));


                // If patching, patch OPatch first
                if (optionsHelper.applyingPatches()) {
                    optionsHelper.addOPatch1394ToImage(tmpDir, opatchBugNumber);
                }

                // resolve required patches
                cmdBuilder.addAll(optionsHelper.handlePatchFiles(null));

                // Copy wls response file to tmpDir
                copyResponseFilesToDir(tmpDir);
                Utils.setOracleHome(installerResponseFile, dockerfileOptions);

            }



            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(tmpDir + File.separator + "Dockerfile",
                "Rebase_Image.mustache", dockerfileOptions, dryRun);

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
        List<InstallerFile> retVal = new ArrayList<>();
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



    /**
     * Determines the support password by parsing the possible three input options.
     *
     * @return String form of password
     * @throws IOException in case of error
     */
    private String handlePasswordOptions() throws IOException {
        return Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
    }


    public String getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            tempDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", tempDirectory);
        }
        return tempDirectory;
    }

    public WLSInstallerType getInstallerType() {
        return installerType;
    }

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
        names = {"--sourceImage"},
        description = "Docker image containing source domain."
    )
    private String sourceImage;

    @Option(
        names = {"--targetImage"},
        description = "Docker image with updated JDK or MW Home"
    )
    private String targetImage;

    @Option(
        names = {"--installerResponseFile"},
        description = "path to a response file. Override the default responses for the Oracle installer"
    )
    private String installerResponseFile;

}
