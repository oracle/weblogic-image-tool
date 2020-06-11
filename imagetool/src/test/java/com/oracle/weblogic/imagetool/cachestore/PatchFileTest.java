// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
public class PatchFileTest {
    static Path cacheDir;
    static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static final String BUGNUMBER = "456789";
    static final String SOME_VERSION = "12.2.1.3.0";
    static Level originalLogLevel;

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir) throws IOException {
        PatchFileTest.cacheDir = cacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        // build a fake cache with two installers
        String key1 = BUGNUMBER + "_" + SOME_VERSION;
        Path path1 = tempDir.resolve("patch1.zip");
        String key2 = "wls_12.2.1.4.0";
        Path path2 = tempDir.resolve("installer.file.122140.jar");
        cacheStore.addToCache(key1, path1.toString());
        cacheStore.addToCache(key2, path2.toString());
        Files.write(path1, fileContents);

        // disable console logging
        LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
        originalLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterAll
    static void teardown() {
        // restore original logging level after this test suite completes
        LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
        logger.setLevel(originalLogLevel);
    }

    @Test
    void simpleVersion() {
        PatchFile p1 = new PatchFile(BUGNUMBER, SOME_VERSION, null,null, null);
        assertEquals(SOME_VERSION, p1.getVersion(), "simple patch did not return version provided");
        assertEquals(BUGNUMBER, p1.getBugNumber(), "simple patch did not return bug number provided");
    }

    @Test
    void derivedVersion() {
        PatchFile p1 = new PatchFile(BUGNUMBER + "_7890", SOME_VERSION, null,null, null);
        assertEquals("7890", p1.getVersion(), "simple patch did not return derived version");
        assertEquals(BUGNUMBER, p1.getBugNumber(), "simple patch did not return derived bug number");
    }

    @Test
    void resolveFile() throws IOException, XPathExpressionException {
        // resolve should fail for a PatchFile that is not in the store
        PatchFile p1 = new PatchFile("99999", SOME_VERSION, null,null, null);
        assertThrows(FileNotFoundException.class, () -> p1.resolve(cacheStore));

        // PatchFile resolve should result in the same behavior has getting the path from the cache store
        PatchFile patch2 = new PatchFile(BUGNUMBER, SOME_VERSION, null,null, null);
        String expected = cacheStore.getValueFromCache(BUGNUMBER + "_" + SOME_VERSION);
        assertEquals(expected, patch2.resolve(cacheStore), "failed to resolve patch in cache");
    }


    private static class TestPatchFile extends PatchFile {
        TestPatchFile(String patchId, String version, String patchSetVersion, String userid, String password) {
            super(patchId, version, patchSetVersion, userid, password);
        }

        @Override
        public void downloadFile(String url, String fileName, String username, String password) throws IOException {
            Path newFile = cacheDir.resolve(fileName);
            Files.write(newFile, fileContents);
        }
    }


    private PatchFile getPatchFileWithAruInfo(String patchId, String version, String psuVer, String aruXml)
        throws IOException, NoSuchFieldException, IllegalAccessException, XPathExpressionException {

        Field aruInfoField = PatchFile.class.getDeclaredField("aruInfo");
        aruInfoField.setAccessible(true);
        assertNull(cacheStore.getValueFromCache(patchId), "ERROR, patch should not exist in cache before test starts");
        PatchFile patchFile = new TestPatchFile(patchId, version, psuVer, "myname@sample.org", "pass");
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
            this.getClass().getResourceAsStream(aruXml)))) {

            String aruInfo = buffer.lines().collect(Collectors.joining("\n"));
            aruInfoField.set(patchFile, HttpUtil.parseXmlString(aruInfo));
        }
        patchFile.initPatchInfo();
        return patchFile;
    }

    @Test
    void gettingNewPatch() throws Exception {
        /*
         * Condition:
         *     Patch number is provided with version.
         *     ARU result contains multiple versions of the patch.
         * Expected:
         *     Patch is found and stored based on version provided in patch ID string.
         */
        String patchId = "1110001_12.2.1.3.0";
        // ARU contains three patch versions 12.2.1.1.0, 12.2.1.2.0, 12.2.1.3.0
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.3.0", null,"/patch-1110001.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId);
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        assertEquals("600000000073715", patchFile.getReleaseNumber(), "Patch did not find release number");
    }

    @Test
    void gettingNewPatchWithoutVersion() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     ARU result contains only one version of the patch.
         *     ARU result does not contain matching version of the installer version.
         * Expected:
         *     Version is ignored, and overridden by value from ARU.
         *     Patch is stored using version in ARU (not provided installer version).
         */
        String patchId = "1110002";
        // patch version in XML is actually 12.2.1.1.0, code will warn user and override patch version
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.2.0", null,"/patch-1110002.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.1.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        assertEquals("600000000055130", patchFile.getReleaseNumber(), "Patch did not find release number");
    }

    @Test
    void multiplePatchVersionsNoVersionSupplied() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     ARU result contains multiple versions of the patch.
         * Expected:
         *     Version is derived from installer version.
         *     Patch is found based on installer version.
         */
        String patchId = "1110001";
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.3.0", null,"/patch-1110001.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        assertEquals("600000000073715", patchFile.getReleaseNumber(), "Patch did not find release number");
    }

    @Test
    void psuInvolvedNoVersionSupplied() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     The user is updating an image that already has a PSU.
         *     The patch requested does not have a PSU version in ARU.
         * Expected:
         *     It should select the installer version of the patch.
         */

        // 1110001 has multiple patches, but NO patches for a PSU
        String patchId = "1110001";
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.3.0", "12.2.1.3.181016","/patch-1110001.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        // if equal, found patch for 12.2.1.3.0
        assertEquals("600000000073715", patchFile.getReleaseNumber(), "Selected patch was not correct");
    }

    @Test
    void psuInvolvedNoVersionSuppliedHasPsuVersions() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     The user is updating an image that already has a PSU.
         * Expected:
         *     if the patch does not have PSU patch version in ARU, it should select the installer version of the patch.
         *     if the patch has a PSU patch version in ARU, it should select the PSU version of the patch.
         */

        // 1110003 has multiple patches, and most are for a different PSUs of 12.2.1.3.0
        String patchId = "1110003";
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.3.0", "12.2.1.3.181016","/patch-1110003.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.181016");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        // if equal, found patch for 12.2.1.3.181016
        assertEquals("6000000000737118101602", patchFile.getReleaseNumber(), "Selected patch was not correct");
    }

    @Test
    void psuInvolvedNoVersionSuppliedNoAru() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     The user is updating an image that already has a PSU.
         *     User is working offline.
         * Expected:
         *     It should select the installer version of the patch.
         */

        // 1110001 has multiple patches, but NO patches for a PSU
        String patchId = BUGNUMBER;
        PatchFile patchFile = new TestPatchFile(patchId, SOME_VERSION, "12.2.1.3.181016",null, null);

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from cache");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_" + SOME_VERSION);
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }
}
