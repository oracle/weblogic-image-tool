// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.oracle.weblogic.imagetool.cachestore.OPatchFile.DEFAULT_BUG_NUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
public class CachedFileTest {

    static Path cacheDir;
    static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static final String SOME_VERSION = "12.2.1.3.0";

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir) throws IOException {
        CachedFileTest.cacheDir = cacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        // build a fake cache with two installers
        String key1 = "wls_" + SOME_VERSION;
        Path path1 = tempDir.resolve("installer.file.122130.jar");
        Files.write(path1, fileContents);
        cacheStore.addToCache(key1, path1.toString());
        cacheStore.addToCache("wls_12.2.1.4.0", "/dont/care");

        // OPatch files
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.0.0", "/dont/care");
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.4.0.0", "/dont/care");
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.2.2", "/dont/care");
    }

    @Test
    void versionString() {
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, SOME_VERSION);

        assertEquals("wls_" + SOME_VERSION, wlsInstallerFile.getKey(),
            "Cached file getKey failed for WLS installer");

        assertEquals(SOME_VERSION, wlsInstallerFile.getVersion(), "CachedFile returned wrong version");

        // if the file ID has the version separator, CachedFile should ignore the version passed, and use the version
        // in the ID.
        CachedFile cf = new CachedFile("something_versionString", SOME_VERSION);
        assertEquals("something_versionString", cf.getKey(),
            "Cached file getKey failed for version string");

        // getVersion should always return the version that was provided in the constructor, not the one in the ID
        assertEquals(SOME_VERSION, cf.getVersion(), "CachedFile returned wrong version");
    }

    @Test
    void resolveFile() throws Exception {
        // resolve should fail for a CachedFile that is not in the store
        CachedFile fakeFile = new CachedFile(InstallerType.WLS, "10.3.6.0.0");
        assertThrows(FileNotFoundException.class, () -> fakeFile.resolve(cacheStore));

        // CachedFile resolve should result in the same behavior has getting the path from the cache store
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, SOME_VERSION);
        String expected = cacheStore.getValueFromCache("wls_" + SOME_VERSION);
        assertEquals(expected, wlsInstallerFile.resolve(cacheStore), "resolve failed for CachedFile");
    }

    @Test
    void copyFile(@TempDir Path contextDir) throws Exception {
        LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, SOME_VERSION);
            // copy the file from the cache store to the fake build context directory
            Path result = wlsInstallerFile.copyFile(cacheStore, contextDir.toString());
            // check to see if the file was copied correctly by examining the contents of the resulting file
            assertLinesMatch(fileContents, Files.readAllLines(result),
                "copied file contents do not match source");
        } finally {
            logger.setLevel(oldLevel);
        }
    }

    @Test
    void latestOpatchVersion() throws IOException, AruException, XPathExpressionException {
        // OPatch file should default to the default OPatch bug number and the latest version found in cache
        OPatchFile patchFile = OPatchFile.getInstance(null, null, null, cacheStore);
        assertEquals(DEFAULT_BUG_NUM + "_13.9.4.0.0", patchFile.getKey(),
            "failed to get latest Opatch version from the cache");
    }

    @Test
    void specificOpatchVersion() throws IOException, AruException, XPathExpressionException {
        // OPatch file should default to the default OPatch bug number and the latest version found in cache
        OPatchFile patchFile = OPatchFile.getInstance(DEFAULT_BUG_NUM + "_13.9.2.2.2", null, null, cacheStore);
        assertEquals(DEFAULT_BUG_NUM + "_13.9.2.2.2", patchFile.getKey(),
            "failed to get specific Opatch version from the cache");
    }
}
