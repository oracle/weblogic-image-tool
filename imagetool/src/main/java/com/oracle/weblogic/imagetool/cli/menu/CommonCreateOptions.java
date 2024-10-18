// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;
import static com.oracle.weblogic.imagetool.util.BuildPlatform.AMD64;
import static com.oracle.weblogic.imagetool.util.BuildPlatform.ARM64;
import static com.oracle.weblogic.imagetool.util.Constants.CTX_JDK;

public class CommonCreateOptions extends CommonPatchingOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonCreateOptions.class);

    /**
     * Copy the Java and Middleware installers into the build context directory and set Dockerfile options accordingly.
     */
    void prepareNewImage() throws IOException, InterruptedException, XPathExpressionException, AruException {

        logger.entering();
        copyOptionsFromImage();
        UserSettingsFile settingsFile = new UserSettingsFile();

        List<String> buildPlatforms = getBuildPlatform();
        // Verify version and installers exists first
        verifyInstallers(settingsFile, buildPlatforms);

        if (dockerfileOptions.installJava()) {

            List<String> jdkFilePathList = new ArrayList<>();
            for (String jdkPlatform : buildPlatforms) {
                String buildContextDestination = buildDir();
                if (jdkPlatform.equals(AMD64)) {
                    buildContextDestination = buildContextDestination + "/" + CTX_JDK + AMD64;
                    dockerfileOptions.setTargetAMDPlatform(true);
                } else if (jdkPlatform.equals(ARM64)) {
                    buildContextDestination = buildContextDestination + "/" + CTX_JDK + ARM64;
                    dockerfileOptions.setTargetARMPlatform(true);
                }
                //CachedFile jdk = new CachedFile(InstallerType.JDK, jdkVersion, jdkPlatform);
                //Path installerPath = jdk.copyFile(cache(), buildContextDestination);
                InstallerMetaData installerMetaData = settingsFile.getInstallerForPlatform(InstallerType.JDK,
                    jdkPlatform, jdkVersion);
                Path installerPath = Paths.get(installerMetaData.getLocation());
                Files.copy(installerPath, Paths.get(buildContextDestination).resolve(installerPath.getFileName()));
                jdkFilePathList.add(installerPath.getFileName().toString());
            }
            // TODO:  Why we need this?
            dockerfileOptions.setJavaInstaller(jdkFilePathList);
        }

        if (dockerfileOptions.installMiddleware()) {
            MiddlewareInstall install =
                new MiddlewareInstall(getInstallerType(), installerVersion, installerResponseFiles, buildPlatforms);
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


    void verifyInstallers(UserSettingsFile settingsFile, List<String> buildPlatforms) throws IOException {

        // Verify version and installers exists first
        for (String buildPlatform : buildPlatforms) {
            InstallerMetaData jdkInstallerMetaData = settingsFile.getInstallerForPlatform(InstallerType.JDK,
                buildPlatform, jdkVersion);
            if (jdkInstallerMetaData == null) {
                throw new IOException("Could not find installer for jdk " + jdkVersion + "  " + buildPlatform);
            } else {
                // If needed
                verifyInstallerHash(jdkInstallerMetaData);
            }

            for (InstallerType installerType : getInstallerType().installerList()) {
                InstallerMetaData installerMetaData = settingsFile.getInstallerForPlatform(installerType,
                    buildPlatform, installerVersion);
                if (installerMetaData == null) {
                    throw new IOException(String.format("Could not find installer type %s, platform %s and version %s",
                        installerType, buildPlatform, installerVersion));
                } else {
                    // If needed
                    verifyInstallerHash(installerMetaData);
                }
            }
        }

    }

    private static void verifyInstallerHash(InstallerMetaData installerMetaData) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        try (FileInputStream fis = new FileInputStream(installerMetaData.getLocation())) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        String hashString = sb.toString();
        if (!hashString.equals(installerMetaData.getHash())) {
            throw new IOException(String.format("Installer hash mismatch, expected %s but got %s for file %s",
                installerMetaData.getHash(), hashString, installerMetaData.getLocation()));
        }
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