// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.aru.InstalledPatch;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.wdt.WdtOperation;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

@Command(
    name = "update",
    description = "Update an existing docker image that was previously created with the image tool",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class UpdateImage extends CommonPatchingOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(UpdateImage.class);

    @Override
    public CommandResponse call() throws Exception {
        logger.finer("Entering UpdateImage.call ");
        Instant startTime = Instant.now();

        try {
            initializeOptions();

            if (Utils.isEmptyString(fromImage())) {
                return CommandResponse.error("IMG-0100");
            }

            dockerfileOptions.setBaseImage(fromImage()).setWdtBase(fromImage());

            Properties baseImageProperties = Utils.getBaseImageProperties(buildEngine, fromImage(),
                "/probe-env/inspect-image-long.sh", buildDir());

            dockerfileOptions.setJavaHome(baseImageProperties.getProperty("javaHome", null));

            String oracleHome = baseImageProperties.getProperty("oracleHome", null);
            if (oracleHome == null) {
                return CommandResponse.error("IMG-0072", fromImage());
            }
            dockerfileOptions.setOracleHome(oracleHome);

            if (wdtOptions.userProvidedFiles() && !wdtOptions.modelOnly()) {
                String domainHome = baseImageProperties.getProperty("domainHome", null);
                if (domainHome == null && wdtOperation == WdtOperation.UPDATE) {
                    return CommandResponse.error("IMG-0071", fromImage());
                }
            }

            installerVersion = baseImageProperties.getProperty("wlsVersion", Constants.DEFAULT_WLS_VERSION);

            String opatchVersion = baseImageProperties.getProperty("opatchVersion");

            String baseImageUsr = baseImageProperties.getProperty("oracleHomeUser");
            String baseImageGrp = baseImageProperties.getProperty("oracleHomeGroup");

            if (isChownSet()) {
                // --chown for UPDATE no longer makes sense if the value must always be what the fromImage is.
                if (!dockerfileOptions.userid().equals(baseImageUsr)
                    || !dockerfileOptions.groupid().equals(baseImageGrp)) {
                    // if the user specified a --chown that did not match the fromImage(), error
                    return CommandResponse.error("IMG-0087", fromImage(), baseImageUsr, baseImageGrp);
                }
            } else {
                // if the user did not specify --chown, use the user:group for the Oracle Home found in the --fromImage
                dockerfileOptions.setUserId(baseImageUsr);
                dockerfileOptions.setGroupId(baseImageGrp);
                logger.fine("--chown not set by user. Using values found in --fromImage {0} for --chown {1}:{2}",
                    fromImage(), baseImageUsr, baseImageGrp);
            }

            List<InstalledPatch> installedPatches = Collections.emptyList();

            if (applyingPatches()) {

                String userId = getUserId();
                String password = getPassword();

                if (shouldUpdateOpatch()) {
                    OPatchFile opatchFile = OPatchFile.getInstance(opatchBugNumber, userId, password, cache());
                    String opatchFilePath = opatchFile.resolve(cache());

                    // if there is a newer version of OPatch than contained in the image, update OPatch
                    if (Utils.compareVersions(opatchVersion, opatchFile.getVersion()) < 0) {
                        logger.info("IMG-0008", opatchVersion, opatchFile.getVersion());
                        String filename = new File(opatchFilePath).getName();
                        Files.copy(Paths.get(opatchFilePath), Paths.get(buildDir(), filename));
                        dockerfileOptions.setOPatchPatchingEnabled();
                        dockerfileOptions.setOPatchFileName(filename);
                    } else {
                        logger.info("IMG-0074", opatchVersion, opatchFile.getVersion());
                    }
                }

                logger.finer("Verifying Patches to WLS ");

                if (userId == null) {
                    logger.info("IMG-0009");
                } else {
                    if (!Utils.validatePatchIds(patches, false)) {
                        return CommandResponse.error("Patch ID validation failed");
                    }

                    String oraclePatches = baseImageProperties.getProperty("oraclePatches", null);
                    if (oraclePatches != null) {
                        if (oraclePatches.contains("OPatch failed")) {
                            logger.severe("patch inventory = " + oraclePatches);
                            return CommandResponse.error("opatch lsinventory failed");
                        }
                        installedPatches = InstalledPatch.getPatchList(oraclePatches);
                    } else {
                        return CommandResponse.error("lsinventory missing. required to check for conflicts");
                    }
                }
            }

            BuildCommand cmdBuilder = getInitialBuildCmd(buildDir());

            // build wdt args if user passes --wdtModelPath
            wdtOptions.handleWdtArgs(dockerfileOptions, buildDir());
            dockerfileOptions.setWdtCommand(wdtOperation);
            if (dockerfileOptions.runRcu()
                && (wdtOperation == WdtOperation.UPDATE || wdtOperation == WdtOperation.DEPLOY)) {
                return CommandResponse.error("IMG-0055");
            }

            setImageInstallerType(baseImageProperties.getProperty("oracleInstalledProducts"));
            if (imageInstallerType == null && applyingLatestPsu()) {
                // This error occurred with the 12.2.1.4 quick slim image because registry.xml was missing data
                logger.warning("IMG-0096", fromImage());
            } else {
                logger.info("IMG-0094", getInstallerType());
            }
            handlePatchFiles(installedPatches);

            // create dockerfile
            String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
                "Update_Image.mustache", dockerfileOptions, dryRun);

            runDockerCommand(dockerfile, cmdBuilder);
            if (!dryRun) {
                wdtOptions.handleResourceTemplates(imageTag());
            }
        } catch (Exception ex) {
            return CommandResponse.error(ex.getMessage());
        } finally {
            cleanup();
        }
        Instant endTime = Instant.now();
        logger.finer("Exiting UpdateImage.call ");

        if (dryRun) {
            return CommandResponse.success("IMG-0054");
        } else {
            return CommandResponse.success("IMG-0053",
                Duration.between(startTime, endTime).getSeconds(), imageTag());
        }
    }

    private void setImageInstallerType(String value) {
        imageInstallerType = FmwInstallerType.fromProductList(value);
    }

    @Override
    FmwInstallerType getInstallerType() {
        if (isInstallerTypeSet() || imageInstallerType == null) {
            return super.getInstallerType();
        } else {
            return imageInstallerType;
        }
    }

    private FmwInstallerType imageInstallerType;

    @Override
    String getInstallerVersion() {
        return installerVersion;
    }

    private String installerVersion;

    @Option(
        names = {"--wdtOperation"},
        description = "Create a new domain, or update an existing domain.  Default: ${DEFAULT-VALUE}. "
            + "Supported values: ${COMPLETION-CANDIDATES}"
    )
    private WdtOperation wdtOperation = WdtOperation.UPDATE;

    @ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtFullOptions wdtOptions = new WdtFullOptions();
}