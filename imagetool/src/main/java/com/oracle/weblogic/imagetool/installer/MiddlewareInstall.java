// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class MiddlewareInstall {

    private static final LoggingFacade logger = LoggingFactory.getLogger(MiddlewareInstall.class);

    private List<MiddlewareInstallPackage> installerFiles = new ArrayList<>();
    private FmwInstallerType fmwInstallerType;

    /**
     * Get the install metadata for a given middleware install type.
     * @param type the requested middleware install type
     */
    public MiddlewareInstall(FmwInstallerType type, String version, List<Path> responseFiles, String buildPlatform)
        throws FileNotFoundException {

        logger.info("IMG-0039", type.installerListString(), version);
        fmwInstallerType = type;

        for (InstallerType installer : type.installerList()) {
            MiddlewareInstallPackage pkg = new MiddlewareInstallPackage();
            pkg.type = installer;
            pkg.installer = new CachedFile(installer, version, buildPlatform);
            pkg.responseFile = new DefaultResponseFile(installer, type);
            addInstaller(pkg);
        }
        setResponseFiles(responseFiles);
    }

    private static String getJarNameFromInstaller(Path installerFile) throws IOException {
        String filename = installerFile.getFileName().toString();
        logger.entering(filename);
        if (filename == null) {
            return null;
        }

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
     * @param cacheStore cache where the installers are defined.
     * @param buildContextDir the directory where the installers should be copied.
     * @throws IOException if any of the copy commands fails.
     */
    public void copyFiles(CacheStore cacheStore, String buildContextDir) throws IOException {
        logger.entering();
        for (MiddlewareInstallPackage installPackage: installerFiles) {
            Path filePath = installPackage.installer.copyFile(cacheStore, buildContextDir);
            installPackage.installerFilename = filePath.getFileName().toString();
            installPackage.jarName = getJarNameFromInstaller(filePath);
            installPackage.isZip = installPackage.installerFilename.endsWith(".zip");
            installPackage.isBin = installPackage.jarName.endsWith(".bin");
            installPackage.responseFile.copyFile(buildContextDir);
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
