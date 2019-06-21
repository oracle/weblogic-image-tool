// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "update",
        description = "Update WebLogic docker image with selected patches",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage extends ImageOperation {

    private final Logger logger = Logger.getLogger(UpdateImage.class.getName());

    public UpdateImage() {
        super();
    }

    public UpdateImage(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() throws Exception {
        logger.finer("Entering UpdateImage.call ");
        Instant startTime = Instant.now();

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
                dockerfileOptions.setBaseImage(fromImage);
            }

            tmpDir2 = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            logger.info("tmp directory in for docker run: " + tmpDir2);
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

            // We need to find out the actual version number of the opatchBugNumber - what if useCache=always ?
            String opatchBugNumberVersion = "";

            if (useCache == CachePolicy.ALWAYS) {
                String opatchFile = cacheStore.getValueFromCache(opatchBugNumber + "_opatch");
                if (opatchFile != null ) {
                    opatchBugNumberVersion = Utils.getOpatchVersionFromZip(opatchFile);
                    logger.info(String.format("OPatch patch number %s cached file %s version %s", opatchBugNumber,
                        opatchFile, opatchBugNumberVersion ));
                } else {
                    String msg = String.format("OPatch patch number --opatchBugNumber %s cannot be found in cache",
                        opatchBugNumber);
                    logger.severe(msg);
                    throw new IOException(msg);
                }
            } else {
                opatchBugNumberVersion = ARUUtil.getOPatchVersionByBugNumber(opatchBugNumber, userId, password);
            }

            opatch_1394_required = (latestPSU || !patches.isEmpty()) &&
                (Utils.compareVersions(installerVersion, Constants.DEFAULT_WLS_VERSION) >= 0 &&
                    Utils.compareVersions(opatchVersion, opatchBugNumberVersion) < 0);

            //Do not update or install packages in offline only mode
            if (useCache != CachePolicy.ALWAYS) {
                String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
                if (!Utils.isEmptyString(pkgMgr)) {
                    dockerfileOptions.setPackageInstaller(pkgMgr);
                }
            }

            baseImageProperties.keySet().forEach(x -> logger.info(x + "=" + baseImageProperties.getProperty(x.toString())));

            if (latestPSU || !patches.isEmpty()) {
                logger.finer("Verifying Patches to WLS ");
                if (useCache == CachePolicy.ALWAYS) {
                    logger.warning("skipping patch conflict check. useCache set to " + useCache);
                } else {
                    String lsInvFile = tmpDir2.toAbsolutePath().toString() + File.separator + "opatch-lsinventory.txt";
                    if (Files.exists(Paths.get(lsInvFile)) && Files.size(Paths.get(lsInvFile)) > 0) {
                        logger.info("opatch-lsinventory file exists at: " + lsInvFile);
                        Set<String> toValidateSet = new HashSet<>();
                        if (latestPSU) {
                            toValidateSet.add(ARUUtil.getLatestPSUNumber(installerType.toString(), installerVersion,
                                    userId, password));
                        }
                        toValidateSet.addAll(patches);
                        ValidationResult validationResult = ARUUtil.validatePatches(lsInvFile,
                                new ArrayList<>(toValidateSet), installerType.toString(), installerVersion, userId,
                                password);
                        if (!validationResult.isSuccess()) {
                            return new CommandResponse(-1, validationResult.getErrorMessage());
                        } else {
                            logger.info("patch conflict check successful");
                        }
                    } else {
                        return new CommandResponse(-1, "inventory file missing. required to check for conflicts");
                    }
                }
            }

            // create a tmp directory for user.
            tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            // create dockerfile
            Utils.writeDockerfile(tmpDirPath + File.separator + "Dockerfile", "/docker-files/Update_Image.mustache", dockerfileOptions);

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));
            Utils.runDockerCommand(isCLIMode, cmdBuilder, dockerLog);

        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            Utils.deleteFilesRecursively(tmpDir);
            Utils.deleteFilesRecursively(tmpDir2);
        }
        Instant endTime = Instant.now();
        logger.finer("Exiting UpdateImage.call ");

        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds() + "s. image tag: " + imageTag);
    }

    @Override
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        if (opatch_1394_required) {
            addOPatch1394ToImage(tmpDir, opatchBugNumber);
        }
        return super.handlePatchFiles(tmpDir, tmpPatchesDir);
    }

    @Option(
            names = {"--fromImage"},
            description = "Docker image to use as base image.",
            required = true
    )
    private String fromImage;

    @Option(
        names = {"--opatchBugNumber"},
        description = "use this opatch patch bug number",
        defaultValue = "28186730"
    )
    private String opatchBugNumber;

    private boolean opatch_1394_required = false;
}
