// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
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

    private static final LoggingFacade logger = LoggingFactory.getLogger(UpdateImage.class);

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

        String tmpDir = null;

        try {

            CommandResponse result = super.call();
            if (result.getStatus() != 0) {
                return result;
            }

            if (fromImage == null || fromImage.isEmpty()) {
                return new CommandResponse(-1, "update requires a base image. use --fromImage to specify base image");
            }

            dockerfileOptions.setBaseImage(fromImage);

            tmpDir = getTempDirectory();
            Utils.copyResourceAsFile("/probe-env/test-update-env.sh",
                    tmpDir + File.separator + "test-env.sh", true);

            Properties baseImageProperties = Utils.getBaseImageProperties(fromImage, tmpDir);

            String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
            if (!Utils.isEmptyString(pkgMgr)) {
                dockerfileOptions.setPackageInstaller(pkgMgr);
            }

            String oracleHome = baseImageProperties.getProperty("ORACLE_HOME", null);
            if (oracleHome == null) {
                return new CommandResponse(-1, "ORACLE_HOME env variable undefined in base image: " + fromImage);
            }
            installerType = WLSInstallerType.fromValue(baseImageProperties.getProperty("WLS_TYPE",
                    WLSInstallerType.WLS.toString()));
            installerVersion = baseImageProperties.getProperty("WLS_VERSION", Constants.DEFAULT_WLS_VERSION);

            String opatchVersion = baseImageProperties.getProperty("OPATCH_VERSION");

            // We need to find out the actual version number of the opatchBugNumber - what if useCache=always ?
            String opatchBugNumberVersion;

            if (userId == null && password == null) {
                String opatchFile = cacheStore.getValueFromCache(opatchBugNumber + "_opatch");
                if (opatchFile != null) {
                    opatchBugNumberVersion = Utils.getOpatchVersionFromZip(opatchFile);
                    logger.info("IMG-0008", opatchBugNumber, opatchFile, opatchBugNumberVersion);
                } else {
                    String msg = String.format("OPatch patch number --opatchBugNumber %s cannot be found in cache",
                            opatchBugNumber);
                    logger.severe(msg);
                    throw new IOException(msg);
                }
            } else {
                opatchBugNumberVersion = ARUUtil.getOPatchVersionByBugNumber(opatchBugNumber, userId, password);
            }

            opatchRequired = (latestPSU || !patches.isEmpty())
                    && (Utils.compareVersions(installerVersion, Constants.DEFAULT_WLS_VERSION) >= 0
                    && Utils.compareVersions(opatchVersion, opatchBugNumberVersion) < 0);

            baseImageProperties.keySet().forEach(x ->
                    logger.info(x + "=" + baseImageProperties.getProperty(x.toString())));

            if (latestPSU || !patches.isEmpty()) {
                logger.finer("Verifying Patches to WLS ");
                if (userId == null) {
                    logger.warning("IMG-0009");
                } else {

                    if (!Utils.validatePatchIds(patches, false)) {
                        return new CommandResponse(-1, "Patch ID validation failed");
                    }

                    String b64lsout = baseImageProperties.getProperty("LSINV_TEXT", null);
                    if (b64lsout != null) {
                        // remove space in the string to make a valid b64 string
                        b64lsout = b64lsout.replace(" ", "");

                        byte[] lsinventoryContent = Base64.getDecoder().decode(b64lsout);
                        String lsinventoryText = new String(lsinventoryContent);

                        logger.finest("ls inventory = " + lsinventoryText);

                        // Any better way to do this ?

                        if (lsinventoryText.contains("OPatch failed")) {
                            logger.severe("ls inventory = " + lsinventoryText);
                            return new CommandResponse(-1, "lsinventory failed");
                        }
                        Set<String> toValidateSet = new HashSet<>();
                        if (latestPSU) {
                            toValidateSet.add(ARUUtil.getLatestPSUNumber(installerType.toString(), installerVersion,
                                    userId, password));
                            if (installerType.toString().equals(Constants.INSTALLER_FMW)) {
                                toValidateSet.add(ARUUtil.getLatestPSUNumber(Constants.INSTALLER_WLS, installerVersion,
                                    userId, password));
                            }
                        }
                        toValidateSet.addAll(patches);
                        ValidationResult validationResult =
                                ARUUtil.validatePatches(lsinventoryText,
                                        new ArrayList<>(toValidateSet), installerType.toString(), installerVersion,
                                        userId,
                                        password);
                        if (!validationResult.isSuccess()) {
                            return new CommandResponse(-1, validationResult.getErrorMessage());
                        } else {
                            logger.info("IMG-0006");
                        }
                    } else {
                        return new CommandResponse(-1, "lsinventory missing. required to check for conflicts");
                    }

                }
            }

            // create a tmp patch directory.
            Path tmpPatchesDir = createPatchesTempDirectory(tmpDir);

            List<String> cmdBuilder = getInitialBuildCmd();

            // this handles wls, jdk and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWdtArgsIfRequired(tmpDir));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            // create dockerfile
            Utils.writeDockerfile(
                    tmpDir + File.separator + "Dockerfile", "Update_Image.mustache", dockerfileOptions);
            // add directory to pass the context
            cmdBuilder.add(tmpDir);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));
            Utils.runDockerCommand(isCLIMode, cmdBuilder, dockerLog);

        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            if (cleanup) {
                Utils.deleteFilesRecursively(tmpDir);
            }
        }
        Instant endTime = Instant.now();
        logger.finer("Exiting UpdateImage.call ");

        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds()
                + "s" + ". image tag: " + imageTag);
    }

    @Override
    List<String> handlePatchFiles(String tmpDir, Path tmpPatchesDir) throws Exception {
        if (opatchRequired) {
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

    private boolean opatchRequired = false;
}
