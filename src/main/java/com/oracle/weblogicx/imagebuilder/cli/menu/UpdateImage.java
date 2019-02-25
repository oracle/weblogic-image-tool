/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.menu;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import com.oracle.weblogicx.imagebuilder.util.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@Command(
        name = "update",
        description = "Update WebLogic docker image with selected patches",
        version = "1.0",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage extends ImageOperation {

    private final Logger logger = Logger.getLogger(UpdateImage.class.getName());

    public UpdateImage() {
    }

    public UpdateImage(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() throws Exception {
        Instant startTime = Instant.now();

        FileHandler fileHandler = setupLogger(isCLIMode);
        Path tmpDir = null;
        Path tmpDir2 = null;

        try {

            CommandResponse result = super.call();
            if (result.getStatus() != 0) {
                return result;
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            if (fromImage == null || fromImage.isEmpty()) {
                return new CommandResponse(-1, "update requires a base image. use --fromImage to specify base image");
            } else {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add("BASE_IMAGE=" + fromImage);
            }

            tmpDir2 = Files.createTempDirectory(Paths.get(System.getProperty("user.home")), null);
            logger.info("tmp directory in user.home for docker run: " + tmpDir2);
            Utils.copyResourceAsFile("/probe-env/test-update-env.sh",
                    tmpDir2.toAbsolutePath().toString() + File.separator + "test-env.sh", true);

            List<String> imageEnvCmd = Utils.getDockerRunCmd(tmpDir2, fromImage, "test-env.sh");

            Properties baseImageProperties = Utils.runDockerCommand(imageEnvCmd);

            String oracleHome = baseImageProperties.getProperty("ORACLE_HOME", null);
            if (oracleHome == null) {
                return new CommandResponse(-1, "ORACLE_HOME env variable undefined in base image: " + fromImage);
            }
            installerType = WLSInstallerType.fromValue(baseImageProperties.getProperty("WLS_TYPE", WLSInstallerType.WLS.toString()));
            installerVersion = baseImageProperties.getProperty("WLS_VERSION", Constants.DEFAULT_WLS_VERSION);

            String opatchVersion = baseImageProperties.getProperty("OPATCH_VERSION");
            opatch_1394_required = (Utils.compareVersions(installerVersion, Constants.DEFAULT_WLS_VERSION) >= 0 &&
                    Utils.compareVersions(opatchVersion, "13.9.4.0.0") < 0);

            String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
            if (!Utils.isEmptyString(pkgMgr)) {
                filterStartTags.add(pkgMgr);
            }

            baseImageProperties.keySet().forEach(x -> logger.info(x + "=" + baseImageProperties.getProperty(x.toString())));

            String lsInvFile = tmpDir2.toAbsolutePath().toString() + File.separator + "opatch-lsinventory.txt";
            if (Files.exists(Paths.get(lsInvFile)) && Files.size(Paths.get(lsInvFile)) > 0) {
                logger.info("opatch-lsinventory file exists at: " + lsInvFile);
                Set<String> toValidateSet = new HashSet<>();
                if (latestPSU) {
                    toValidateSet.add(ARUUtil.getLatestPSUNumber(installerType.toString(), installerVersion, userId, password));
                }
                toValidateSet.addAll(patches);
                ValidationResult validationResult = ARUUtil.validatePatches(lsInvFile, new ArrayList<>(toValidateSet),
                        installerType.toString(), installerVersion, userId, password);
                if (!validationResult.isSuccess()) {
                    return new CommandResponse(-1, validationResult.getErrorMessage());
                } else {
                    logger.info("patch conflict check successful");
                }
            } else {
                if (latestPSU || !patches.isEmpty()) {
                    return new CommandResponse(-1, "inventory file missing. required to check for conflicts");
                }
            }

            // create a tmp directory for user.
            tmpDir = Files.createTempDirectory(null);
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            // create dockerfile
            Utils.replacePlaceHolders(tmpDirPath + File.separator + "Dockerfile", "/docker-files/Dockerfile.update", filterStartTags, "/docker-files/Dockerfile.ph");

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));
            Utils.runDockerCommand(cmdBuilder, dockerLog);

        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            Utils.deleteFilesRecursively(tmpDir);
            Utils.deleteFilesRecursively(tmpDir2);
            if (fileHandler != null) {
                fileHandler.close();
                logger.removeHandler(fileHandler);
            }
        }
        Instant endTime = Instant.now();
        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds() + "s. image tag: " + imageTag);
    }

    @Override
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        if (opatch_1394_required) {
            addOPatch1394ToImage(tmpDir);
        }
        return super.handlePatchFiles(tmpDir, tmpPatchesDir);
    }

    @Option(
            names = {"--fromImage"},
            description = "Docker image to use as base image.",
            required = true
    )
    private String fromImage;

    private boolean opatch_1394_required = false;
}
