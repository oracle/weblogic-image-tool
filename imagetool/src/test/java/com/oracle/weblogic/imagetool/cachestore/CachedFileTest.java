// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.BuildPlatform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import static com.oracle.weblogic.imagetool.cachestore.OPatchFile.DEFAULT_BUG_NUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class CachedFileTest {

    static Path cacheDir;
    static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static final String ver12213 = "12.2.1.3.0";

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir) throws IOException {
        Path path12213 = tempDir.resolve("installer.file.122130.jar");
        Files.write(path12213, fileContents);
        Path path12214 = tempDir.resolve("installer.file.12214.jar");
        Files.write(path12214, fileContents);
        Path path1411 = tempDir.resolve("installer.file.141100.jar");
        Files.write(path1411, fileContents);

        CachedFileTest.cacheDir = cacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        // build a fake cache with several installers
        cacheStore.addToCache("wls_" + ver12213, path12213.toString());
        cacheStore.addToCache("wls_12.2.1.4.0_" + BuildPlatform.getPlatformName(), path12214.toString());
        cacheStore.addToCache("wls_14.1.1.0.0_amd64", path1411.toString());

        // OPatch files
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.0.0", "/not/used");
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.4.0.0", "/not/used");
        cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.2.2", "/not/used");
    }

    //@Test
    void versionString() {
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, "12.2.1.3.0");

        assertEquals("12.2.1.3.0", wlsInstallerFile.getVersion(),
            "CachedFile should return the version from the constructor");
    }

    //@Test
    void userProvidedPatchVersionAsId() {
        // User provided a patch ID with the version string in the ID
        CachedFile cf = new CachedFile(true,"something_versionString", "12.2.1.2.0",
            null);
        // if the patch ID has the version, CachedFile should ignore the installer version passed to the constructor,
        // and use the version in the ID.
        assertEquals("something_versionString", cf.getPatchId(),
            "CachedFile getKey() failed when version string was provided by the user in the ID");

        // getVersion should always return the version that was provided in the constructor, not the one in the ID
        assertEquals("12.2.1.2.0", cf.getVersion(), "CachedFile returned wrong version");
    }

    //@Test
    void resolveFileNotFound() throws Exception {
        // resolve should fail for a CachedFile that is not in the store
        CachedFile fakeFile = new CachedFile(InstallerType.WLS, "10.3.6.0.0");
        assertThrows(FileNotFoundException.class, () -> fakeFile.resolve());
    }

    //@Test
    void resolveFileFindsFile() throws IOException {
        // Resolve a CachedFile stored in the cache (created in test setup above)
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, ver12213);
        String expected = cacheStore.getValueFromCache("wls_" + ver12213);
        assertEquals(expected, wlsInstallerFile.resolve(), "CachedFile did not resolve file");
    }

    //@Test
    void resolveNoArchFile() throws IOException {
        // Look for a cache entry where the user specified the architecture/platform amd64
        CachedFile wlsNoArch = new CachedFile(InstallerType.WLS, ver12213, "amd64");

        // verify the cache is setup as expected.
        // wls_12.2.1.3.0 is in the cache, but wls_12.2.1.3.0_amd64 is NOT in the cache
        assertNull(cacheStore.getValueFromCache("wls_12.2.1.3.0_amd64"));
        String expected = cacheStore.getValueFromCache("wls_12.2.1.3.0");
        assertNotNull(expected);

        assertEquals(expected, wlsNoArch.resolve(), "CachedFile returned wrong file");
    }

    //@Test
    void resolveWithArchitecture() throws IOException {
        // Look for a cache entry where the user specified the architecture/platform amd64
        CachedFile wlsArch = new CachedFile(InstallerType.WLS, "14.1.1.0.0", "amd64");

        // verify the cache is setup as expected.  wls_14.1.1.0.0_amd64 is in the cache
        String expected = cacheStore.getValueFromCache("wls_14.1.1.0.0_amd64");
        assertNotNull(expected);

        assertEquals(expected, wlsArch.resolve(), "CachedFile failed to find specific architecture");
    }

    //@Test
    void resolveFallbackToLocalArch() throws IOException {
        // Look for a cache entry where the user did not specify the architecture/platform
        CachedFile wlsArch = new CachedFile(InstallerType.WLS, "12.2.1.4.0");

        // verify the cache is setup as expected.  wls_14.1.1.0.0_amd64 is in the cache, but wls_14.1.1.0.0 is not
        assertNull(cacheStore.getValueFromCache("wls_12.2.1.4.0"));
        String expected = cacheStore.getValueFromCache("wls_12.2.1.4.0_" + BuildPlatform.getPlatformName());
        assertNotNull(expected);

        assertEquals(expected, wlsArch.resolve(), "CachedFile failed to check local architecture");
    }

    //@Test
    void copyFile(@TempDir Path contextDir) throws Exception {
        LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, ver12213);
            // copy the file from the cache store to the fake build context directory
            Path result = wlsInstallerFile.copyFile(cacheStore, contextDir.toString());
            // check to see if the file was copied correctly by examining the contents of the resulting file
            assertLinesMatch(fileContents, Files.readAllLines(result),
                "copied file contents do not match source");
        } finally {
            logger.setLevel(oldLevel);
        }
    }

    ////@Test
    //void latestOpatchVersion() throws IOException, AruException, XPathExpressionException {
    //    // OPatch file should default to the default OPatch bug number and the latest version found in cache
    //    OPatchFile patchFile = OPatchFile.getInstance(null, null, null, cacheStore);
    //    assertEquals(DEFAULT_BUG_NUM + "_13.9.4.0.0", patchFile.getPatchId(),
    //        "failed to get latest Opatch version from the cache");
    //}
    //
    ////@Test
    //void specificOpatchVersion() throws IOException, AruException, XPathExpressionException {
    //    // OPatch file should default to the default OPatch bug number and the latest version found in cache
    //    OPatchFile patchFile = OPatchFile.getInstance(DEFAULT_BUG_NUM + "_13.9.2.2.2", null, null, cacheStore);
    //    assertEquals(DEFAULT_BUG_NUM + "_13.9.2.2.2", patchFile.getPatchId(),
    //        "failed to get specific Opatch version from the cache");
    //}
}
