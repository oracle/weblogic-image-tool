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
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.util.Constants.AMD64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.ARM64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.CTX_JDK;
import static com.oracle.weblogic.imagetool.util.Constants.DEFAULT_JDK_VERSION;
import static com.oracle.weblogic.imagetool.util.Constants.DEFAULT_WLS_VERSION;

public class CommonCreateOptions extends CommonPatchingOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonCreateOptions.class);

    /**
     * Copy the Java and Middleware installers into the build context directory and set Dockerfile options accordingly.
     */
    void prepareNewImage() throws IOException, InterruptedException, XPathExpressionException, AruException {

        logger.entering();
        copyOptionsFromImage();


        List<String> buildPlatforms = getBuildPlatform();
        // Verify version and installers exists first
        verifyInstallers(buildPlatforms);

        if (dockerfileOptions.installJava()) {

            List<String> jdkFilePathList = new ArrayList<>();
            for (String jdkPlatform : buildPlatforms) {
                String buildContextDestination = buildDir();
                Architecture arch = Architecture.fromString(jdkPlatform);
                if (jdkPlatform.equals(AMD64_BLD)) {
                    buildContextDestination = buildContextDestination + "/" + CTX_JDK + AMD64_BLD;
                    dockerfileOptions.setTargetAMDPlatform(true);
                } else if (jdkPlatform.equals(ARM64_BLD)) {
                    buildContextDestination = buildContextDestination + "/" + CTX_JDK + ARM64_BLD;
                    dockerfileOptions.setTargetARMPlatform(true);
                }
                //CachedFile jdk = new CachedFile(InstallerType.JDK, jdkVersion, jdkPlatform);
                //Path installerPath = jdk.copyFile(cache(), buildContextDestination);
                InstallerMetaData installerMetaData = ConfigManager.getInstance()
                    .getInstallerForPlatform(InstallerType.JDK, arch, jdkVersion);
                Path installerPath = Paths.get(installerMetaData.getLocation());
                Files.copy(installerPath, Paths.get(buildContextDestination).resolve(installerPath.getFileName()));
                jdkFilePathList.add(installerPath.getFileName().toString());
            }
            dockerfileOptions.setJavaInstaller(jdkFilePathList);
        }

        if (dockerfileOptions.installMiddleware()) {
            MiddlewareInstall install =
                new MiddlewareInstall(getInstallerType(), installerVersion, installerResponseFiles, buildPlatforms,
                    buildEngine);
            install.copyFiles(buildDir());
            dockerfileOptions.setMiddlewareInstall(install);
            dockerfileOptions.includeBinaryOsPackages(getInstallerType().equals(FmwInstallerType.OHS));
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

    void verifyInstallers(List<String> buildPlatforms) throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        // Verify version and installers exists first
        for (String buildPlatform : buildPlatforms) {
            Architecture arch = Architecture.fromString(buildPlatform);
            jdkVersion = resetInstallerVersion(InstallerType.JDK, jdkVersion);
            verifyInstallerExists(configManager, InstallerType.JDK, arch, jdkVersion, buildPlatform);
            if (getInstallerType().equals(FmwInstallerType.OID)) {
                verifyOIDInstallers(configManager, arch, buildPlatform, installerVersion);
            } else {
                verifyNormalInstallers(getInstallerType().installerList(), configManager, arch, buildPlatform);
            }
        }

    }

    void verifyOIDInstallers(ConfigManager configManager, Architecture arch,
                             String buildPlatform, String installerVersion) throws IOException {
        verifyInstallerExists(configManager, InstallerType.OID, arch, installerVersion, buildPlatform);
        InstallerMetaData installerMetaData = configManager.getInstallerForPlatform(InstallerType.OID,
            arch, installerVersion);
        String baseFMWVersion = installerMetaData.getBaseFMWVersion();
        verifyInstallerExists(configManager, InstallerType.FMW, arch, baseFMWVersion, buildPlatform);
    }

    void verifyNormalInstallers(List<InstallerType> installers, ConfigManager configManager, Architecture arch,
                                String buildPlatform) throws IOException {
        for (InstallerType installerType : installers) {
            installerVersion = resetInstallerVersion(installerType, installerVersion);
            verifyInstallerExists(configManager, installerType, arch, installerVersion, buildPlatform);
        }
    }

    void verifyInstallerExists(ConfigManager configManager, InstallerType installerType, Architecture arch,
                               String installerVersion, String buildPlatform) throws IOException {

        logger.info("IMG-0150", installerType, installerVersion, arch);

        InstallerMetaData installerMetaData = configManager.getInstallerForPlatform(installerType,
            arch, installerVersion);
        if (installerMetaData == null) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0145", installerType,
                buildPlatform, installerVersion));
        } else {
            // If needed
            verifyInstallerHash(installerMetaData);
        }
    }

    private String resetJDKVersion(String jdkVersion) {
        String defaultJDKVersion = ConfigManager.getInstance().getDefaultJDKVersion();
        if (defaultJDKVersion != null) {
            if (DEFAULT_JDK_VERSION.equals(jdkVersion)) {
                jdkVersion = defaultJDKVersion;
            }
        }
        return jdkVersion;
    }

    private String resetInstallerVersion(InstallerType installerType, String installerVersion) {
        String fixedVersion = installerVersion;
        String defaultVersion;

        switch (installerType) {
            case JDK:
                String defaultJDKVersion = ConfigManager.getInstance().getDefaultJDKVersion();
                if (defaultJDKVersion != null) {
                    if (DEFAULT_JDK_VERSION.equals(installerVersion)) {
                        fixedVersion = defaultJDKVersion;
                    }
                }
                break;
            case WLS:
                String defaultWLSVersion = ConfigManager.getInstance().getDefaultWLSVersion();
                if (defaultWLSVersion != null) {
                    if (DEFAULT_WLS_VERSION.equals(installerVersion)) {
                        fixedVersion = defaultWLSVersion;
                    }
                }
                break;
            case WDT:
                defaultVersion = ConfigManager.getInstance().getDefaultWDTVersion();
                if (defaultVersion != null && installerVersion == null) {
                    fixedVersion = defaultVersion;
                }
                break;
            default:
                break;
        }
        return fixedVersion;
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
        if (!hashString.equalsIgnoreCase(installerMetaData.getDigest())) {
            throw new IOException(String.format("Installer hash mismatch, expected %s but got %s for file %s",
                installerMetaData.getDigest(), hashString, installerMetaData.getLocation()));
        }
    }

    String getInstallerVersion() {
        return installerVersion;
    }

    @Option(
        names = {"--version"},
        description = "Installer version. Default: ${DEFAULT-VALUE}",
        required = true,
        defaultValue = DEFAULT_WLS_VERSION
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