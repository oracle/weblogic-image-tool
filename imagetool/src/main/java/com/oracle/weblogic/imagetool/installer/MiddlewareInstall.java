// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.FmwInstallerType;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class MiddlewareInstall {

    private static final LoggingFacade logger = LoggingFactory.getLogger(MiddlewareInstall.class);

    private List<MiddlewareInstallPackage> installerFiles = new ArrayList<>();

    /**
     * Get the install metadata for a given middleware install type.
     * @param type the requested middleware install type
     */
    public MiddlewareInstall(FmwInstallerType type, String version) {
        List<InstallerType> list = type.getInstallerList();

        String listStr = list.stream().map(Object::toString).collect(Collectors.joining(", "));
        logger.info("IMG-0039", listStr, version);

        for (InstallerType installer : list) {
            MiddlewareInstallPackage pkg = new MiddlewareInstallPackage();
            pkg.installer = new CachedFile(installer, version);
            pkg.responseFile = new DefaultResponseFile("/response-files/" + installer.toString() + ".rsp");
            addInstaller(pkg);
        }
    }

    private static String getJarNameFromInstaller(Path installerFile) throws IOException {
        String filename = installerFile.getFileName().toString();
        logger.entering(filename);
        if (filename == null) {
            return null;
        }

        if (filename.endsWith(".zip")) {
            logger.finer("locating installer JAR inside installer ZIP");
            ZipFile zipFile = new ZipFile(installerFile.toFile());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                logger.finer("Entry in zip {0}: {1}", filename, entryName);
                if (entryName.endsWith(".jar")) {
                    filename = entryName;
                    break;
                }
            }
        }
        logger.exiting(filename);
        return filename;
    }

    public void copyFiles(CacheStore cacheStore, String buildContextDir) throws IOException {
        logger.entering();
        for (MiddlewareInstallPackage installPackage: installerFiles) {
            Path filePath = installPackage.installer.copyFile(cacheStore, buildContextDir);
            installPackage.installerFilename = filePath.getFileName().toString();
            installPackage.jarName = getJarNameFromInstaller(filePath);
            installPackage.isZip = installPackage.installerFilename.endsWith(".zip");
            installPackage.responseFile.copyFile(buildContextDir);
        }
        logger.exiting();
    }

    public List<MiddlewareInstallPackage> getInstallers() {
        return installerFiles;
    }

    boolean addInstaller(MiddlewareInstallPackage installPackage) {
        return installerFiles.add(installPackage);
    }

    public void setResponseFiles(List<String> responseFiles) {
        if (responseFiles != null && !responseFiles.isEmpty()) {
            if (responseFiles.size() != installerFiles.size()) {
                throw new IllegalArgumentException(
                    "The number of response files did not match the number of installers " + installerFiles.size());
            }
            for (int i = 0; i < installerFiles.size(); i++) {
                installerFiles.get(i).responseFile = new ProvidedResponseFile(responseFiles.get(i));
            }
        }
    }
}
