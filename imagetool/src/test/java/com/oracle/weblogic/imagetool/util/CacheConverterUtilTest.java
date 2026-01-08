// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CacheConverterUtilTest {

    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static Path path12213;
    static Path patch;
    static UnitTestLogHandler logHandler = new UnitTestLogHandler();
    static Pattern installerPattern;

    @BeforeAll
    static void setup(@TempDir Path tempDir) throws IOException {
        path12213 = tempDir.resolve("installer.file.122130.jar");
        Files.write(path12213, fileContents);
        patch = tempDir.resolve("patch.file.122130.jar");
        Files.write(patch, fileContents);

        TestSetup.setup(tempDir);

        installerPattern = Pattern.compile(CacheConverterUtil.INSTALLER_PATTERN);
    }

    @BeforeEach
    void setupLogger() {
        LoggingFacade logger = LoggingFactory.getLogger(CacheConverterUtil.class);

        logger.getUnderlyingLogger().setUseParentHandlers(false);
        logger.getUnderlyingLogger().addHandler(logHandler);
        logger.setLevel(Level.ALL);
        logHandler.clear();
    }

    @AfterEach
    void tearDownLogger() {
        LoggingFacade logger = LoggingFactory.getLogger(CacheConverterUtil.class);
        logger.getUnderlyingLogger().removeHandler(logHandler);
        logger.getUnderlyingLogger().setUseParentHandlers(true);
    }

    @Test
    void testConvertVersionString() {
        assertEquals("12.2.1.4.0", CacheConverterUtil.convertVersionString("122140"));
        assertEquals("14.1.1.0.0", CacheConverterUtil.convertVersionString("141100"));
        assertEquals("12.2.1.4.0", CacheConverterUtil.convertVersionString("12.2.1.4.0"));
    }

    @Test
    void testParseInstallerValidWithoutArch() {
        String line = "wls_12.2.1.3.0=" + path12213.toString();

        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertInstallerEntry(
            installerPattern, line);
        assertNotNull(result);

        assertEquals(InstallerType.WLS.toString().toLowerCase(), result.getKey().toLowerCase());
        assertEquals("12.2.1.3.0", result.getVersion());
        assertEquals(Utils.standardPlatform(Architecture.getLocalArchitecture().toString()), result.getArchitecture());
        assertEquals(path12213.toString(), result.getFilePath());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), result.getFileDate());

    }

    @Test
    void testParseInstallerValidWithArch() {
        String line = "wls_12.2.1.3.0_amd64=" + path12213.toString();

        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertInstallerEntry(
            installerPattern, line);
        assertNotNull(result);

        assertEquals(InstallerType.WLS.toString().toLowerCase(), result.getKey().toLowerCase());
        assertEquals("12.2.1.3.0", result.getVersion());
        assertEquals("amd64", result.getArchitecture());
        assertEquals(path12213.toString(), result.getFilePath());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), result.getFileDate());

    }

    @Test
    void testParseInstallerWithFakeFilePath() {

        String line = "wls_12.2.1.3.0_amd64=/tmp/nosuchfile.zip";

        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertInstallerEntry(
            installerPattern, line);
        assertNull(result);
        List<LogRecord> recs = logHandler.getRecords();
        boolean found = false;
        for (LogRecord rec : recs) {
            if (rec.getMessage().equals("IMG-0131")) {
                assertEquals("/tmp/nosuchfile.zip", rec.getParameters()[0]);
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void testParseInstallerInvalidKey() {
        String line = "invalid_122140=/fake/path.jar";
        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertInstallerEntry(
            installerPattern, line);
        assertNull(result);
        List<LogRecord> recs = logHandler.getRecords();
        boolean found = false;
        for (LogRecord rec : recs) {
            //IMG-0129=Cannot parse image tool version 1 metadata file line: unrecognized product {0} in line {1}.
            if (rec.getMessage().equals("IMG-0129")) {
                assertEquals(line, rec.getParameters()[1]);
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void testParseInstallerBadFormat() {
        String line = "wls_12.2.1.4.0";
        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertInstallerEntry(
            installerPattern, line);
        assertNull(result);
        List<LogRecord> recs = logHandler.getRecords();
        boolean found = false;
        for (LogRecord rec : recs) {
            //IMG-0128=Cannot parse image tool version 1 metadata file line: {0}, skipping.
            if (rec.getMessage().equals("IMG-0128")) {
                assertEquals(line, rec.getParameters()[0]);
                found = true;
            }
        }
        assertTrue(found);
    }


    @Test
    void testParsePatchValid() {
        String line = "12345678_12.2.1.4.0=" + patch.toString();
        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertPatchEntry(line);
        assertNotNull(result);

        assertEquals("12345678", result.getKey());
        assertEquals("12.2.1.4.0", result.getVersion());
        assertEquals(patch.toString(), result.getFilePath());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), result.getFileDate());
        assertEquals(Utils.standardPlatform(Architecture.getLocalArchitecture().toString()),
            result.getArchitecture());
    }

    @Test
    void testParsePatchWithShortVersionAndArch() {
        String line = "12345678_122140_amd64=" + patch.toString();

        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertPatchEntry(line);
        assertNotNull(result);

        assertEquals("12345678", result.getKey());
        assertEquals("12.2.1.4.0", result.getVersion());
        assertEquals(patch.toString(), result.getFilePath());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), result.getFileDate());
        assertEquals("amd64", result.getArchitecture());
    }

    @Test
    void testParsePatchBadFormat() {
        String line = "12345678";
        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertPatchEntry(line);
        assertNull(result);
        List<LogRecord> recs = logHandler.getRecords();
        boolean found = false;
        for (LogRecord rec : recs) {
            //IMG-0128=Cannot parse image tool version 1 metadata file line: {0}, skipping.
            if (rec.getMessage().equals("IMG-0128")) {
                assertEquals(line, rec.getParameters()[0]);
                found = true;
            }
        }
        assertTrue(found);
    }


    @Test
    void testParsePatchFileNotExist() {
        String line = "12345678_12.2.1.4.0=/nonexistent.zip";
        CacheConverterUtil.ParsedInfo result = CacheConverterUtil.convertPatchEntry(line);
        assertNull(result);
        List<LogRecord> recs = logHandler.getRecords();
        boolean found = false;
        for (LogRecord rec : recs) {
            if (rec.getMessage().equals("IMG-0131")) {
                assertEquals("/nonexistent.zip", rec.getParameters()[0]);
                found = true;
            }
        }
        assertTrue(found);
    }

}
