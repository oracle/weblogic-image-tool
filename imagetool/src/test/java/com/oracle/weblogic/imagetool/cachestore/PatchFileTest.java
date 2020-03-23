// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
public class PatchFileTest {
    private static final CacheStore cacheStore = new CacheStoreImpl();
    private static final List<String> fileContents = Arrays.asList("A", "B", "C");
    private static final String BUGNUMBER = "123456";
    private static final String SOME_VERSION = "12.2.1.3.0";

    @BeforeAll
    static void setup(@TempDir Path tempDir) throws IOException {
        // build a fake cache with two installers
        String key1 = BUGNUMBER + "_" + SOME_VERSION;
        Path path1 = tempDir.resolve("patch1.zip");
        String key2 = "wls_12.2.1.4.0";
        Path path2 = tempDir.resolve("installer.file.122140.jar");
        cacheStore.addToCache(key1, path1.toString());
        cacheStore.addToCache(key2, path2.toString());
        Files.write(path1, fileContents);
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
        LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            // resolve should fail for a PatchFile that is not in the store
            PatchFile p1 = new PatchFile("99999", SOME_VERSION, null, null);
            assertThrows(FileNotFoundException.class, () -> p1.resolve(cacheStore));

            // PatchFile resolve should result in the same behavior has getting the path from the cache store
            PatchFile patch2 = new PatchFile(BUGNUMBER, SOME_VERSION, null, null);
            String expected = cacheStore.getValueFromCache(BUGNUMBER + "_" + SOME_VERSION);
            assertEquals(expected, patch2.resolve(cacheStore), "failed to resolve patch in cache");
        } finally {
            logger.setLevel(oldLevel);
        }
    }
}
