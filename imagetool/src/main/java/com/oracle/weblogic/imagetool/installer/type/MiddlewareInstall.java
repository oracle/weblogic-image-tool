// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer.type;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.oracle.weblogic.imagetool.api.model.FmwInstallerType;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstallPackage;
import com.oracle.weblogic.imagetool.installer.ProvidedResponseFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class MiddlewareInstall {

    private static final LoggingFacade logger = LoggingFactory.getLogger(MiddlewareInstall.class);

    private List<MiddlewareInstallPackage> installerFiles = new ArrayList<>();

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

    /**
     * Get the install metadata for a given middleware install type.
     * @param type the requested middleware install type
     * @return the metadata for the given install type
     */
    public static MiddlewareInstall getInstall(FmwInstallerType type, String version) {
        MiddlewareInstall result;

        switch (type) {
            case WLS:
                result =  new MiddlewareInstallWLS(version);
                break;
            case FMW:
                result = new MiddlewareInstallFMW(version);
                break;
            case SOA:
                result = new MiddlewareInstallSOA(version);
                break;
            case OSB:
                result = new MiddlewareInstallOSB(version);
                break;
            case SOAOSB:
                result = new MiddlewareInstallSOAOSB(version);
                break;
            default:
                throw new IllegalArgumentException(type.toString() + " is not a supported middleware install type");
        }
        return result;
    }

    public void copyFiles(CacheStore cacheStore, String buildContextDir) throws IOException {
        logger.entering();
        for (MiddlewareInstallPackage installPackage: installerFiles) {
            Path filePath = installPackage.installer.copyFile(cacheStore, buildContextDir);
            installPackage.installerFilename = filePath.getFileName().toString();
            installPackage.jarName = getJarNameFromInstaller(filePath);
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
