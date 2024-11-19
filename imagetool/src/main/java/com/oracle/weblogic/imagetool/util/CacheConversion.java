// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

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
                        //ConfigManager.getInstance().addPatch(key, arch, filepath, version);
                        // TODO
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
                        InstallerMetaData metaData = new InstallerMetaData(arch, filepath,
                            Utils.getSha256Hash(filepath), Utils.getTodayDate(), version);
                        //ConfigManager.getInstance().addInstaller(InstallerType.valueOf(key), version, metaData);
                        //// TODO
                    } else {
                        logger.warning("IMG-0128", line);
                    }

                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        CacheConversion cacheConversion = new CacheConversion();
        cacheConversion.convert("/Users/JSHUM/dimtemp23/cache/.metadata");
    }
}
