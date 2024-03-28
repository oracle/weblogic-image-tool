// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

public class CommonCreateOptions extends CommonPatchingOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonCreateOptions.class);

    /**
     * Copy the Java and Middleware installers into the build context directory and set Dockerfile options accordingly.
     */
    void prepareNewImage() throws IOException, InterruptedException, XPathExpressionException, AruException {

        logger.entering();
        copyOptionsFromImage();

        if (dockerfileOptions.installJava()) {
            CachedFile jdk = new CachedFile(InstallerType.JDK, jdkVersion, getBuildPlatform());
            Path installerPath = jdk.copyFile(cache(), buildDir());
            dockerfileOptions.setJavaInstaller(installerPath.getFileName().toString());
        }

        if (dockerfileOptions.installMiddleware()) {
            MiddlewareInstall install =
                new MiddlewareInstall(getInstallerType(), installerVersion, installerResponseFiles, getBuildPlatform());
            install.copyFiles(cache(), buildDir());
            dockerfileOptions.setMiddlewareInstall(install);
        } else {
            dockerfileOptions.setWdtBase("os_update");
        }

        // resolve required patches
        handlePatchFiles();

        // If patching, patch OPatch first
        if (applyingPatches() && shouldUpdateOpatch()) {
            prepareOpatchInstaller(buildDir(), opatchBugNumber);
        }

        Utils.setOracleHome(installerResponseFiles, dockerfileOptions);

        // Set the inventory oraInst.loc file location (null == default location)
        dockerfileOptions.setInvLoc(inventoryPointerInstallLoc);

        // Set the inventory location, so that it will be copied
        if (inventoryPointerFile != null) {
            Utils.setInventoryLocation(inventoryPointerFile, dockerfileOptions);
            Utils.copyLocalFile(Paths.get(inventoryPointerFile), Paths.get(buildDir(), "/oraInst.loc"));
        } else {
            Utils.copyResourceAsFile("/response-files/oraInst.loc", buildDir());
        }
        logger.exiting();
    }

    String getInstallerVersion() {
        return installerVersion;
    }

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
}