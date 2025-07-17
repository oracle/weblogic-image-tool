// Copyright (c) 2025, Oracle and/or its affiliates.
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

import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;


public class CacheConverterUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CacheConverterUtil.class);
    public static final String INSTALLER_PATTERN = "^(wls|fmw|ohs|wlsdev|wlsslim|soa|osb|b2b|mft|idm|db19|"
            + "oud|oid|wcc|wcp|wcs|jdk|wls|odi|wdt)$";
    public static final String IMG_0128 = "IMG-0128";
    public static final String IMG_0129 = "IMG-0129";

    /**
     * convert cache file to new format.
     * @param inputFile input cache file
     * @throws IOException when error
     */
    public static void convert(String inputFile) throws IOException {
        Pattern installerPattern = Pattern.compile(INSTALLER_PATTERN);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.charAt(0) == '#') {
                    continue;
                }
                logger.info("IMG-0137", line);
                // patches
                if (Character.isDigit(line.charAt(0))) {
                    handlePatchPattern(line);
                } else {
                    // installer
                    handleInstallerPattern(installerPattern, line);
                }
            }
        }
    }

    public static class ParsedInfo {
        private final String filePath;
        private final String key;
        private final String version;
        private final String architecture;
        private final String fileDate;

        /**
         * Constructor.
         * @param filePath File Path
         * @param key  Installer type
         * @param version Version
         * @param architecture Architecture
         * @param fileDate File date
         */
        public ParsedInfo(String filePath, String key, String version, String architecture, String fileDate) {
            this.filePath = filePath;
            this.key = key;
            this.version = version;
            this.architecture = architecture;
            this.fileDate = fileDate;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getKey() {
            return key;
        }

        public String getVersion() {
            return version;
        }

        public String getArchitecture() {
            return architecture;
        }

        public String getFileDate() {
            return fileDate;
        }

    }

    /**
     * Convert installer line v1 to v2.
     * @param installerPattern Installer pattern
     * @param line  input line
     * @return ParsedInfo for temporary holder
     */
    public static ParsedInfo convertInstallerEntry(Pattern installerPattern, String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2) {
            String filepath = tokens[1];
            String key = null;
            String version = null;
            String arch = null;
            tokens = tokens[0].split("_");

            if (tokens.length == 2) {
                key = tokens[0];
                version = tokens[1];
                Matcher matcher = installerPattern.matcher(key);
                if (!matcher.matches()) {
                    logger.warning(IMG_0129, key, line);
                    return null;
                }
            } else if (tokens.length == 3) {
                key = tokens[0];
                version = convertVersionString(tokens[1]);
                arch = tokens[2];
                arch = Architecture.fromString(arch).toString();
            } else {
                logger.warning(IMG_0128, line);
                return null;
            }
            String fileDate = getFileDate(filepath);
            if (arch == null) {
                arch = Utils.standardPlatform(Architecture.getLocalArchitecture().toString());
            }
            if (fileDate != null) {
                return new ParsedInfo(filepath, key, version, arch, fileDate);
            }
        } else {
            logger.warning(IMG_0128, line);
            return null;
        }
        return null;
    }


    private static void handleInstallerPattern(Pattern installerPattern, String line) throws IOException {

        ParsedInfo info = convertInstallerEntry(installerPattern, line);
        if (info != null) {
            logger.info("IMG-0147", info.getKey(), info.getVersion(), info.getFilePath(), info.getArchitecture());
            InstallerMetaData metaData = new InstallerMetaData(info.getArchitecture(), info.getFilePath(),
                Utils.getSha256Hash(info.getFilePath()), info.getFileDate(), info.getVersion(), info.getVersion());
            ConfigManager.getInstance().addInstaller(InstallerType.fromString(info.getKey()), info.getVersion(),
                metaData);
        }
    }

    /**
     * Convert patch line v1 to v2.
     * @param line input line
     * @return ParsedInfo for temporary holder
     */
    public static ParsedInfo convertPatchEntry(String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2) {
            String filepath = tokens[1];
            String key = null;
            String version = null;
            String arch = null;
            tokens = tokens[0].split("_");
            if (tokens.length == 2) {
                key = tokens[0];
                version = tokens[1];
            } else if (tokens.length == 3) {
                key = tokens[0];
                version = convertVersionString(tokens[1]);
                arch = tokens[2];
                arch = Architecture.fromString(arch).toString();
            } else {
                logger.warning(IMG_0128, line);
                return null;
            }
            String fileDate = getFileDate(filepath);
            if (arch == null) {
                arch = Utils.standardPlatform(Architecture.getLocalArchitecture().toString());
            }
            if (fileDate != null) {
                return new ParsedInfo(filepath, key, version, arch, fileDate);
            }
        } else {
            logger.warning(IMG_0128, line);
        }
        return null;
    }

    private static void handlePatchPattern(String line) throws IOException {
        ParsedInfo info = convertPatchEntry(line);
        if (info != null) {
            logger.info("IMG-0148", info.getKey(), info.getVersion(), info.getFilePath());
            ConfigManager.getInstance().addPatch(info.getKey(), info.getArchitecture(),
                info.getFilePath(), info.getVersion(),
                "Converted from v1", info.getFileDate());
        }
    }

    /**
     * Convert a version string to standard format.
     * @param version input string
     * @return converted version format string
     */
    public static String convertVersionString(String version) {
        if (version != null && version.length() == 6) {
            // e.g. 122140 141100
            return String.format("%s.%s.%s.%s.%s",
                version.substring(0,2),
                version.substring(2,3),
                version.substring(3,4),
                version.substring(4,5),
                version.substring(5));
        } else {
            return version;
        }
    }

    private static String getFileDate(String filepath) {
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


}
