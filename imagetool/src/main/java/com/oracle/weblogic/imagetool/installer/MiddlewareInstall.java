// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;

import static com.oracle.weblogic.imagetool.util.Constants.AMD64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.ARM64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.CTX_FMW;

public class MiddlewareInstall {

    private static final LoggingFacade logger = LoggingFactory.getLogger(MiddlewareInstall.class);

    private final List<MiddlewareInstallPackage> installerFiles = new ArrayList<>();
    private final FmwInstallerType fmwInstallerType;

    /**
     * Get the install metadata for a given middleware install type.
     * @param type the requested middleware install type
     */
    public MiddlewareInstall(FmwInstallerType type, String version, List<Path> responseFiles,
                             List<String> buildPlatform, String buildEngine)
        throws FileNotFoundException {
        logger.info("IMG-0039", type.installerListString(), version);
        fmwInstallerType = type;
        ConfigManager configManager = ConfigManager.getInstance();
        if (buildPlatform == null) {
            buildPlatform = new ArrayList<>();
        }
        if (buildPlatform.isEmpty()) {
            if (configManager.getDefaultBuildPlatform() != null) {
                buildPlatform.add(configManager.getDefaultBuildPlatform());
            } else {
                buildPlatform.add(Architecture.getLocalArchitecture().name());
            }
        }
        Architecture localArchitecture = Architecture.getLocalArchitecture();
        String originalType = type.toString();


        for (InstallerType installerType : type.installerList()) {
            for (String platform : buildPlatform) {

                logger.info("IMG-0153", installerType, version, platform);
                platform = Utils.standardPlatform(platform);
                MiddlewareInstallPackage pkg = new MiddlewareInstallPackage();
                Architecture arch = Architecture.fromString(platform);
                if (localArchitecture != arch) {
                    logger.warning("IMG-0146");
                }
                pkg.type = installerType;
                if (AMD64_BLD.equals(platform)) {
                    pkg.installer = new CachedFile(installerType, version, Architecture.AMD64);
                }
                if (ARM64_BLD.equals(platform)) {
                    pkg.installer = new CachedFile(installerType, version, Architecture.ARM64);
                }

                // get the details from cache of whether there is a base version of WLS required.
                String useVersion = version;
                if (type.installerList().size() > 1 && installerType == InstallerType.FMW) {
                    InstallerMetaData productData = configManager.getInstallerForPlatform(
                        InstallerType.fromString(originalType),
                        Architecture.fromString(platform), version);
                    useVersion = productData.getBaseFMWVersion();
                }

                InstallerMetaData metaData = configManager.getInstallerForPlatform(installerType, arch, useVersion);
                pkg.installerPath = Paths.get(metaData.getLocation());
                pkg.installerFilename = pkg.installerPath.getFileName().toString();
                pkg.responseFile = new DefaultResponseFile(installerType, type);
                pkg.platform = platform;
                if (installerType.equals(InstallerType.DB19)) {
                    pkg.preinstallCommands = Collections.singletonList("34761383/changePerm.sh /u01/oracle");
                }
                addInstaller(pkg);
            }
        }
        setResponseFiles(responseFiles);
    }

    private static String getJarNameFromInstaller(Path installerFile) throws IOException {
        String filename = installerFile.getFileName().toString();
        logger.entering(filename);

        if (filename.endsWith(".zip")) {
            logger.finer("locating installer JAR inside installer ZIP");
            try (ZipFile zipFile = new ZipFile(installerFile.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    logger.finer("Entry in zip {0}: {1}", filename, entryName);
                    if (entryName.endsWith(".jar") || entryName.endsWith(".bin")) {
                        filename = entryName;
                        break;
                    }
                }
            }
        }
        logger.exiting(filename);
        return filename;
    }

    /**
     * Copy all necessary installers to the build context directory.
     * @param buildContextDir the directory where the installers should be copied.
     * @throws IOException if any of the copy commands fails.
     */
    public void copyFiles(String buildContextDir) throws IOException {
        logger.entering();

        for (MiddlewareInstallPackage installPackage: installerFiles) {
            String buildContextDestination = buildContextDir;
            // based on the platform copy to sub directory
            if (installPackage.platform.equals(AMD64_BLD)) {
                buildContextDestination = buildContextDestination + "/" + CTX_FMW + AMD64_BLD;
            } else if (installPackage.platform.equals(ARM64_BLD)) {
                buildContextDestination = buildContextDestination + "/" + CTX_FMW + ARM64_BLD;
            }
            Files.copy(installPackage.installerPath,
                Paths.get(buildContextDestination).resolve(installPackage.installerPath.getFileName()));
            installPackage.jarName = getJarNameFromInstaller(installPackage.installerPath);
            installPackage.isZip = installPackage.installerFilename.endsWith(".zip");
            installPackage.isBin = installPackage.jarName.endsWith(".bin");
            installPackage.responseFile.copyFile(buildContextDestination);
        }
        logger.exiting();
    }

    public List<MiddlewareInstallPackage> getInstallers() {
        return installerFiles;
    }

    private boolean addInstaller(MiddlewareInstallPackage installPackage) {
        return installerFiles.add(installPackage);
    }

    private void setResponseFiles(List<Path> responseFiles) throws FileNotFoundException {
        if (responseFiles == null || responseFiles.isEmpty()) {
            return;
        }
        logger.fine("response files: {0}", responseFiles);

        // make sure the two arrays are the same size for the for-loop that comes next
        if (responseFiles.size() != installerFiles.size()) {
            throw new IllegalArgumentException(
                Utils.getMessage("IMG-0040",
                    fmwInstallerType.installerListString(),
                    responseFiles.size(),
                    installerFiles.size()));
        }

        for (int i = 0; i < installerFiles.size(); i++) {
            Path responseFile = responseFiles.get(i);
            MiddlewareInstallPackage pkg = installerFiles.get(i);
            if (!Files.isRegularFile(responseFile)) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0042", responseFile));
            }
            logger.info("IMG-0041", responseFile, pkg.type);
            pkg.responseFile = new ProvidedResponseFile(responseFile);
        }
    }
}
