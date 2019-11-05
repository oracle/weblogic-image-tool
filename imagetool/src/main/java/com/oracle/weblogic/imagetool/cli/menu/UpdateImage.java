// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.WdtOperation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "update",
        description = "Update an existing docker image that was previously created with the image tool",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage extends ImageOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(UpdateImage.class);

    public UpdateImage() {
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
            String lsinventoryText = null;

            if (applyingPatches()) {

                String opatchBugNumberVersion;

                if (userId == null && password == null) {
                    String opatchFile = cacheStore.getValueFromCache(opatchBugNumber + "_opatch");
                    if (opatchFile != null) {
                        opatchBugNumberVersion = Utils.getOpatchVersionFromZip(opatchFile);
                        logger.info("IMG-0008", opatchBugNumber, opatchFile, opatchBugNumberVersion);
                    } else {
                        String msg = String.format("OPatch patch number --opatchBugNumber %s cannot be found in cache. "
                            + "Please download it manually and add it to the cache.", opatchBugNumber);
                        logger.severe(msg);
                        throw new IOException(msg);
                    }
                } else {
                    opatchBugNumberVersion = ARUUtil.getOPatchVersionByBugNumber(opatchBugNumber, userId, password);
                }

                if (Utils.compareVersions(opatchVersion, opatchBugNumberVersion) < 0) {
                    addOPatch1394ToImage(tmpDir, opatchBugNumber);
                }

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
                        lsinventoryText = new String(lsinventoryContent);

                        logger.finer("ls inventory = " + lsinventoryText);

                        // Any better way to do this ?

                        if (lsinventoryText.contains("OPatch failed")) {
                            logger.severe("ls inventory = " + lsinventoryText);
                            return new CommandResponse(-1, "lsinventory failed");
                        }
                    } else {
                        return new CommandResponse(-1, "lsinventory missing. required to check for conflicts");
                    }

                }
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            // this handles wls, jdk and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWdtArgsIfRequired(tmpDir));
            dockerfileOptions.setWdtCommand(wdtOperation);

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(lsinventoryText));

            // create dockerfile
            String dockerfile = Utils.writeDockerfile(tmpDir + File.separator + "Dockerfile",
                "Update_Image.mustache", dockerfileOptions, dryRun);

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
        logger.finer("Exiting UpdateImage.call ");

        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds()
                + "s" + ". image tag: " + imageTag);
    }

    @Override
    public WLSInstallerType getInstallerType() {
        return installerType;
    }

    @Override
    public String getInstallerVersion() {
        return installerVersion;
    }

    // Installer type and version are not parameters for update.  These are derived from the fromImage.
    private WLSInstallerType installerType;
    private String installerVersion;

    @Option(
            names = {"--fromImage"},
            description = "Docker image to use as base image.",
            required = true
    )
    private String fromImage;

    @Option(
        names = {"--wdtOperation"},
        description = "Create a new domain, or update an existing domain.  Default: ${DEFAULT-VALUE}. "
            + "Supported values: ${COMPLETION-CANDIDATES}"
    )
    private WdtOperation wdtOperation = WdtOperation.CREATE;
}
