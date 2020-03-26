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
        PatchFile p1 = new PatchFile(BUGNUMBER, SOME_VERSION, null, null);
        assertEquals(SOME_VERSION, p1.getVersion(), "simple patch did not return version provided");
        assertEquals(BUGNUMBER, p1.getBugNumber(), "simple patch did not return bug number provided");
    }

    @Test
    void derivedVersion() {
        PatchFile p1 = new PatchFile(BUGNUMBER + "_7890", SOME_VERSION, null, null);
        assertEquals("7890", p1.getVersion(), "simple patch did not return derived version");
        assertEquals(BUGNUMBER, p1.getBugNumber(), "simple patch did not return derived bug number");
    }

    @Test
    void resolveFile() throws IOException {
        // resolve should fail for a PatchFile that is not in the store
        PatchFile p1 = new PatchFile("99999", SOME_VERSION, null, null);
        assertThrows(FileNotFoundException.class, () -> p1.resolve(cacheStore));

        // PatchFile resolve should result in the same behavior has getting the path from the cache store
        PatchFile patch2 = new PatchFile(BUGNUMBER, SOME_VERSION, null, null);
        String expected = cacheStore.getValueFromCache(BUGNUMBER + "_" + SOME_VERSION);
        assertEquals(expected, patch2.resolve(cacheStore), "failed to resolve patch in cache");
    }


    private static class TestPatchFile extends PatchFile {
        TestPatchFile(String patchId, String version, String userid, String password) {
            super(patchId, version, userid, password);
        }

        @Override
        public void downloadFile(String url, String fileName, String username, String password) throws IOException {
            Path newFile = cacheDir.resolve(fileName);
            Files.write(newFile, fileContents);
        }
    }


    private PatchFile getPatchFileWithAruInfo(String patchId, String version, String aruXml)
        throws IOException, NoSuchFieldException, IllegalAccessException {

        Field reader = PatchFile.class.getDeclaredField("aruInfo");
        reader.setAccessible(true);
        assertNull(cacheStore.getValueFromCache(patchId), "ERROR, patch should not exist in cache before test starts");
        PatchFile patchFile = new TestPatchFile(patchId, version, "myname@sample.org", "pass");
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
            this.getClass().getResourceAsStream(aruXml)))) {

            String aruInfo = buffer.lines().collect(Collectors.joining("\n"));
            reader.set(patchFile, HttpUtil.parseXmlString(aruInfo));
        }
        return patchFile;
    }

    @Test
    void gettingNewPatch() throws NoSuchFieldException, IllegalAccessException, IOException {
        String patchId = "1110001_12.2.1.3.0";
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.3.0", "/patch-1110001.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId);
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        assertEquals("600000000073715", patchFile.getReleaseNumber(), "Patch did not find release number");
    }

    @Test
    void gettingNewPatchWithoutVersion() throws NoSuchFieldException, IllegalAccessException, IOException {
        // without the version in the patch ID, the ARU info must contain only one patch
        String patchId = "1110002";
        // patch version in XML is actually 12.2.1.1.0, code will warn user and reset patch version
        PatchFile patchFile = getPatchFileWithAruInfo(patchId, "12.2.1.2.0", "/patch-1110002.xml");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.1.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        assertEquals("600000000055130", patchFile.getReleaseNumber(), "Patch did not find release number");
    }
}
