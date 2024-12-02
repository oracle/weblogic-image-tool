// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;

/**
 * Utility to convert image tool 1.x cache store to 2.0 format
 */
public class CacheConversion {

    private static final LoggingFacade logger = LoggingFactory.getLogger(OPatchFile.class);

    private static final String PATCH_PATTERN = "^(wls|fmw|ohs|wlsdev|wlsslim|soa|osb|b2b|mft|idm|db19|"
        + "oud|oid|wcc|wcp|wcs|jdk|wdt|odi|\\d{8,9})(?:_(\\d\\d(?:\\.\\d){3,8}\\.\\d+)(?:_(.*))?)?=(.*)$";
    private static final String INSTALLER_PATTERN = "^(wls|fmw|ohs|wlsdev|wlsslim|soa|osb|b2b|mft|idm|db19|"
        + "oud|oid|wcc|wcp|wcs|jdk|wdt|odi)_(.*?)(?:_(.*))?=(.*)$";

    /**
     * convert cache file to nee format.
     * @param inputFile input cache file
     * @throws IOException when error
     */
    public void convert(String inputFile) throws IOException {
        Pattern patchPattern = Pattern.compile(PATCH_PATTERN);
        Pattern installerPattern = Pattern.compile(INSTALLER_PATTERN);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.charAt(0) == '#') {
                    continue;
                }
                // patches
                if (Character.isDigit(line.charAt(0))) {
                    Matcher matcher = patchPattern.matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String version = matcher.group(2);
                        String arch = matcher.group(3);
                        String filepath = matcher.group(4);
                        String fileDate = getFileDate(filepath);
                        if (fileDate != null) {
                            ConfigManager.getInstance().addPatch(key, arch, filepath, version,
                                "Converted from v1 in " + fileDate);
                        }
                    } else {
                        logger.warning("IMG-0128", line);
                    }
                } else {
                    // installer
                    Matcher matcher = installerPattern.matcher(line);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String version = matcher.group(2);
                        String arch = matcher.group(3);
                        String filepath = matcher.group(4);
                        String fileDate = getFileDate(filepath);
                        if (fileDate != null) {
                            InstallerMetaData metaData = new InstallerMetaData(arch, filepath,
                                Utils.getSha256Hash(filepath), fileDate, version);
                            ConfigManager.getInstance().addInstaller(InstallerType.valueOf(key), version, metaData);
                        }
                    } else {
                        logger.warning("IMG-0128", line);
                    }

                }
            }
        }
    }

    private String getFileDate(String filepath) {
        try {
            Path path = Paths.get(filepath);
            if (Files.exists(path)) {
                FileTime modifiedTime = Files.getLastModifiedTime(path);
                LocalDate today = modifiedTime.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                return today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                logger.warning("IMG-0131", filepath);
                return null;
            }
        } catch (IOException ioe) {
            logger.warning("IMG-0132", filepath, ioe.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Main for converting v1 .metadata file
     * @param args arguments
     * @throws IOException when error converting .metadata file
     */
    public static void main(String[] args) throws IOException {
        // TODO:  Need some setup for settings first???
        CacheConversion cacheConversion = new CacheConversion();
        String cacheEnv = System.getenv(CacheStore.CACHE_DIR_ENV);
        Path cacheMetaFile;
        if (cacheEnv != null) {
            cacheMetaFile = Paths.get(cacheEnv, ".metadata");
        } else {
            cacheMetaFile = Paths.get(System.getProperty("user.home"), ".metadata");
        }
        if (Files.exists(cacheMetaFile)) {
            cacheConversion.convert(cacheMetaFile.toString());
        } else {
            logger.info("IMG-0133", cacheMetaFile.toString());
        }
    }
}
