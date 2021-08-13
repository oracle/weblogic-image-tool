// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

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
        String buildId = UUID.randomUUID().toString();

        try {
            init(buildId);

            tmpDir = getTempDirectory();

            if (!Utils.validatePatchIds(patches, false)) {
                return new CommandResponse(1, "Patch ID validation failed");
            }

            copyOptionsFromImage(fromImage, tmpDir);

            if (dockerfileOptions.installJava()) {
                CachedFile jdk = new CachedFile(InstallerType.JDK, jdkVersion);
                Path installerPath = jdk.copyFile(cache(), tmpDir);
                dockerfileOptions.setJavaInstaller(installerPath.getFileName().toString());
            }

            if (dockerfileOptions.installMiddleware()) {
                MiddlewareInstall install =
                    new MiddlewareInstall(installerType, installerVersion, installerResponseFiles);
                install.copyFiles(cache(), tmpDir);
                dockerfileOptions.setMiddlewareInstall(install);
            } else {
                dockerfileOptions.setWdtBase(fromImage);
            }

            BuildCommand cmdBuilder = getInitialBuildCmd(tmpDir);
            // build wdt args if user passes --wdtModelPath
            wdtOptions.handleWdtArgs(dockerfileOptions, tmpDir);

            // resolve required patches
            handlePatchFiles(installerType);

            // If patching, patch OPatch first
            if (applyingPatches() && shouldUpdateOpatch()) {
                installOpatchInstaller(tmpDir, opatchBugNumber);
            }

            Utils.setOracleHome(installerResponseFiles, dockerfileOptions);

            // Set the inventory oraInst.loc file location (null == default location)
            dockerfileOptions.setInvLoc(inventoryPointerInstallLoc);

            // Set the inventory location, so that it will be copied
            if (inventoryPointerFile != null) {
                Utils.setInventoryLocation(inventoryPointerFile, dockerfileOptions);
                Utils.copyLocalFile(Paths.get(inventoryPointerFile), Paths.get(tmpDir, "/oraInst.loc"), false);
            } else {
                Utils.copyResourceAsFile("/response-files/oraInst.loc", tmpDir);
            }

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(tmpDir + File.separator + "Dockerfile",
                "Create_Image.mustache", dockerfileOptions, dryRun);

            runDockerCommand(dockerfile, cmdBuilder);
            if (!dryRun) {
                wdtOptions.handleResourceTemplates(imageTag);
            }
        } catch (Exception ex) {
            logger.fine("**ERROR**", ex);
            //TO-DO remove this
            logger.info("Exception occurred: {0}", ex.getMessage());
            ex.printStackTrace();
            return new CommandResponse(1, ex.getMessage());
        } finally {
            if (!skipcleanup) {
                Utils.deleteFilesRecursively(tmpDir);
                Utils.removeIntermediateDockerImages(buildEngine, buildId);
            }
        }
        Instant endTime = Instant.now();
        logger.exiting();
        if (dryRun) {
            return new CommandResponse(0, "IMG-0054");
        } else {
            return new CommandResponse(0, "IMG-0053",
                Duration.between(startTime, endTime).getSeconds(), imageTag);
        }
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
        split = ",",
        description = "path to a response file. Override the default responses for the Oracle installer"
    )
    private List<Path> installerResponseFiles;

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
