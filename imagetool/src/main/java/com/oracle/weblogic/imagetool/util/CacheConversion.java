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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cli.cache.CacheOperation;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import picocli.CommandLine;

/**
 * Utility to convert image tool 1.x cache store to 2.0 format
 */
@CommandLine.Command(
    name = "convert",
    description = "Convert cache settings from v1 to v2",
    sortOptions = false
)
public class CacheConversion extends CacheOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CacheConversion.class);

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
                    handlePatchPattern(patchPattern, line);
                } else {
                    // installer
                    handleInstallerPattern(installerPattern, line);
                }
            }
        }
    }

    private void handleInstallerPattern(Pattern installerPattern, String line) throws IOException {
        Matcher matcher = installerPattern.matcher(line);
        if (matcher.matches()) {
            String key = matcher.group(1);
            String version = matcher.group(2);
            String arch = matcher.group(3);
            String filepath = matcher.group(4);
            String fileDate = getFileDate(filepath);
            if (arch == null) {
                arch = Utils.standardPlatform(Architecture.getLocalArchitecture().toString());
            }
            if (fileDate != null) {
                InstallerMetaData metaData = new InstallerMetaData(arch, filepath,
                    Utils.getSha256Hash(filepath), fileDate, version);
                ConfigManager.getInstance().addInstaller(InstallerType.fromString(key), version, metaData);
            }
        } else {
            logger.warning("IMG-0128", line);
        }
    }

    private void handlePatchPattern(Pattern patchPattern, String line) throws IOException {
        Matcher matcher = patchPattern.matcher(line);
        if (matcher.matches()) {
            String key = matcher.group(1);
            String version = matcher.group(2);
            String arch = matcher.group(3);
            String filepath = matcher.group(4);
            String fileDate = getFileDate(filepath);
            if (arch == null) {
                arch = Utils.standardPlatform(Architecture.getLocalArchitecture().toString());
            }
            if (fileDate != null) {
                ConfigManager.getInstance().addPatch(key, arch, filepath, version,
                    "Converted from v1 in " + fileDate, fileDate);
            }
        } else {
            logger.warning("IMG-0128", line);
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

    private boolean initializeSettingFiles(String directory) {
        boolean success = true;
        logger.entering("Entering initializeSettingFiles " + directory);
        try {

            if (directory != null) {
                Path directoryPath = Paths.get(directory);
                if (!Files.exists(directoryPath)) {
                    createIfNotExists(directoryPath, true);
                }

                Path settingsPath = directoryPath.resolve("settings.yaml");
                if (!Files.exists(settingsPath)) {
                    createNewSettingsYaml(settingsPath);
                }
            }
        } catch (Exception ex) {
            logger.warning("IMG-0138", ex.getLocalizedMessage());
            return false;
        }
        logger.exiting("initializeSettingFiles");
        return success;
    }

    private void createNewSettingsYaml(Path settingsPath) throws IOException {
        logger.fine("No existing settings file creating it");
        createIfNotExists(settingsPath, false);
        Path parentPath = settingsPath.getParent();
        List<String> lines = new ArrayList<>();
        lines.add("installerDirectory: " + parentPath.resolve("installers").toString());
        lines.add("patchDirectory: " + parentPath.resolve("patches").toString());
        createIfNotExists(parentPath.resolve("installers"), true);
        createIfNotExists(parentPath.resolve("patches"), true);
        createIfNotExists(parentPath.resolve("installers.yaml"), false);
        createIfNotExists(parentPath.resolve("patches.yaml"), false);

        Files.write(settingsPath, lines);
    }

    private void createIfNotExists(Path entry, boolean isDir) throws IOException {
        if (Files.exists(entry)) {
            return;
        }
        if (isDir) {
            Files.createDirectory(entry);
        } else {
            Files.createFile(entry);
        }
    }

    @Override
    public CommandResponse call() throws Exception {
        CacheConversion cacheConversion = new CacheConversion();
        String cacheEnv = System.getenv(CacheStore.CACHE_DIR_ENV);
        Path cacheMetaFile;
        if (cacheEnv != null) {
            cacheMetaFile = Paths.get(cacheEnv, ".metadata");
            if (!initializeSettingFiles(cacheMetaFile.getParent().toString())) {
                return CommandResponse.error("IMG-0139");
            }
        } else {
            cacheMetaFile = Paths.get(System.getProperty("user.home"), "cache", ".metadata");
            if (!initializeSettingFiles(Paths.get(System.getProperty("user.home"), ".imagetool").toString())) {
                return CommandResponse.error("IMG-0139");
            }
        }
        if (Files.exists(cacheMetaFile)) {
            cacheConversion.convert(cacheMetaFile.toString());
        } else {
            logger.info("IMG-0133", cacheMetaFile.toString());
        }
        return CommandResponse.success("IMG-0134");
    }

}
