// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.VersionNotFoundException;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class PatchFileTest {
    static Path cacheDir;
    static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static final String BUGNUMBER = "456789";
    static final String SOME_VERSION = "12.2.1.3.0";
    static Level originalLogLevel;

    private static void addToCache(Path tempDir, String key, String filename) throws IOException {
        Path path = tempDir.resolve(filename);
        Files.write(path, fileContents);
        cacheStore.addToCache(key, path.toString());
    }

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir)
        throws IOException, NoSuchFieldException, IllegalAccessException {

        PatchFileTest.cacheDir = cacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        // build a fake cache with fake installers
        addToCache(tempDir, BUGNUMBER + "_" + SOME_VERSION, "patch1.zip");
        addToCache(tempDir, "wls_12.2.1.4.0", "installer.file.122140.jar");
        addToCache(tempDir, "1110003_12.2.1.3.181016", "p1110003_12213181016_Generic.zip");
        addToCache(tempDir, "1110003_12.2.1.3.0", "p1110003_122130_Generic.zip");

        // disable console logging
        LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
        originalLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new TestAruUtil());
    }

    /**
     * Intercept calls to the ARU REST API during unit testing.
     */
    public static class TestAruUtil extends AruUtil {
        private final Map<String, Document> responseCache = new HashMap<>();

        /**
         * Intercept calls to the ARU REST API during unit testing.
         * @throws IOException when XML file cannot be read from the project resources.
         */
        public TestAruUtil() throws IOException {
            responseCache.put("1110001", ResourceUtils.instance().getXmlFromResource("/patch-1110001.xml"));
            responseCache.put("1110002", ResourceUtils.instance().getXmlFromResource("/patch-1110002.xml"));
            responseCache.put("1110003", ResourceUtils.instance().getXmlFromResource("/patch-1110003.xml"));
            responseCache.put("28186730", ResourceUtils.instance().getXmlFromResource("/patch-28186730.xml"));
            responseCache.put("2818673x", ResourceUtils.instance().getXmlFromResource("/patch-2818673x.xml"));
        }

        @Override
        public List<AruPatch> getPatches(String bugNumber, String userId, String password)
            throws XPathExpressionException, AruException, IOException {
            if (userId == null) {
                return super.getPatches(bugNumber, userId, password);
            } else {
                return AruPatch.getPatches(responseCache.get(bugNumber));
            }
        }

        @Override
        public String downloadAruPatch(AruPatch aruPatch, String targetDir, String username, String password)
            throws IOException {
            // download the remote patch file to the local target directory
            String filename = targetDir + File.separator + aruPatch.fileName();
            Path newFile = cacheDir.resolve(filename);
            Files.write(newFile, fileContents);
            return filename;
        }
    }

    @AfterAll
    static void teardown() {
        // restore original logging level after this test suite completes
        LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
        logger.setLevel(originalLogLevel);
    }

    @Test
    void resolveFile() throws IOException {
        // resolve should fail for a PatchFile that is not in the store
        AruPatch aruPatch1 = new AruPatch().patchId("99999").version(SOME_VERSION);
        PatchFile p1 = new PatchFile(aruPatch1, null,null);
        assertThrows(FileNotFoundException.class, () -> p1.resolve(cacheStore));

        // PatchFile resolve should result in the same behavior has getting the path from the cache store
        AruPatch aruPatch2 = new AruPatch().patchId(BUGNUMBER).version(SOME_VERSION);
        PatchFile patch2 = new PatchFile(aruPatch2, null,null);
        String expected = cacheStore.getValueFromCache(BUGNUMBER + "_" + SOME_VERSION);
        assertEquals(expected, patch2.resolve(cacheStore), "failed to resolve patch in cache");
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
        String patchId = "1110001";
        // ARU contains three patch versions 12.2.1.1.0, 12.2.1.2.0, 12.2.1.3.0
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, "x", "x");
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, "12.2.1.3.0", "12.2.1.3.181016", "12.2.1.3.0");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
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
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, "x", "x");
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, null, null, "12.2.1.3.0");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");
        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.1.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
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
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, "x", "x");
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, null, null, "12.2.1.3.0");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");

        //assertEquals("600000000073715", patchFile.getReleaseNumber(), "Patch did not find release number");
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
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, "x", "x");
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, null, "12.2.1.3.181016", "12.2.1.3.0");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");
        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
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

        // 1110003 has multiple patches, and most are for different PSUs of 12.2.1.3.0
        String patchId = "1110003";
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, null, null);
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, null, "12.2.1.3.181016", "12.2.1.3.0");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.181016");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }

    @Test
    void noVersionSuppliedNoAru() throws Exception {
        /*
         * Condition:
         *     Patch number is provided without version.
         *     The user is updating an image that does not have a PSU.
         *     User is working offline.
         * Expected:
         *     It should select the installer version of the patch.
         */

        // 1110001 has multiple patches, but NO patches for a PSU
        String patchId = BUGNUMBER;
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, null, null);
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, null, null, SOME_VERSION);
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, null, null);

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from cache");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_" + SOME_VERSION);
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }

    @Test
    void psuInvolvedVersionSuppliedHasPsuVersions() throws Exception {
        /*
         * Condition:
         *     Patch number is provided with version.
         *     The user is updating an image that already has a PSU.
         * Expected:
         *     The provided version should be selected.
         */

        // 1110003 has multiple patches, and most are for different PSUs of 12.2.1.3.0
        String patchId = "1110003";
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, null, null);
        AruPatch aruPatch = AruPatch.selectPatch(aruPatches, "12.2.1.3.0", "12.2.1.3.181016", "12.2.1.3.1");
        assertNotNull(aruPatch);
        PatchFile patchFile = new PatchFile(aruPatch, "x", "x");

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        String filePathFromCache = cacheStore.getValueFromCache(patchId + "_12.2.1.3.0");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }

    @Test
    void throwsVersionNotFound() throws Exception {
        /*
         * Condition:
         *     Patch number is provided with version.
         *     The user is updating an image that already has a PSU.
         *     The provided version number is not found in for the patch ID from ARU.
         * Expected:
         *     Throws VersionNotFoundException.
         */

        String patchId = "1110002";
        List<AruPatch> aruPatches = AruUtil.rest().getPatches(patchId, "x", "x");
        assertThrows(VersionNotFoundException.class,
            () -> AruPatch.selectPatch(aruPatches, "12.2.1.3.0", "12.2.1.3.181016", "12.2.1.3.1"));
    }

    @Test
    void opatchDefaultTest() throws Exception {
        /*
         * Condition:
         *     There are 5 versions of the OPatch patch.
         *     The user does not specify a version.
         * Expected:
         *     The tool ignores the recommended flag and selects the highest version number (latest).
         */

        // 28186730 has multiple patches available, but none are specified
        String patchId = null;
        OPatchFile patchFile = OPatchFile.getInstance(patchId, "x", "x", cacheStore);

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        assertEquals("13.9.4.2.5", patchFile.getVersion(), "wrong version selected");
        String filePathFromCache = cacheStore.getValueFromCache("28186730_13.9.4.2.5");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }

    @Test
    void opatchProvidedVersionTest() throws Exception {
        /*
         * Condition:
         *     There are 5 versions of the OPatch patch.
         *     The user specifies a specific version.
         * Expected:
         *     The provided version of OPatch is selected.
         */

        // 28186730 has multiple patches available, but none are specified
        String patchId = "28186730_13.9.4.2.5";
        OPatchFile patchFile = OPatchFile.getInstance(patchId, "x", "x", cacheStore);

        assertEquals("13.9.4.2.5", patchFile.getVersion(), "wrong version selected");
    }

    @Test
    void opatchProvidedWrongVersionTest() {
        /*
         * Condition:
         *     There are 5 versions of the OPatch patch.
         *     The user specifies a specific version.
         * Expected:
         *     The provided version of the patch is selected.
         */

        // 28186730 has multiple patches available, but none are specified
        String patchId = "28186730_13.9.4.2.2";
        assertThrows(VersionNotFoundException.class, () ->
            OPatchFile.getInstance(patchId, "x", "x", cacheStore));
    }


    @Test
    void opatchNoRecommendedTest() throws Exception {
        /*
         * Condition:
         *     There are 5 versions of the OPatch patch.
         *     None are marked as life_cycle = Recommended.
         *     The user does not specify a specific version.
         * Expected:
         *     The newest OPatch from ARU should be selected.
         */

        // 28186730 has multiple patches available, but none are specified
        String patchId = "2818673x";
        OPatchFile patchFile = OPatchFile.getInstance(patchId, "x", "x", cacheStore);

        String filePath = patchFile.resolve(cacheStore);

        assertNotNull(filePath, "Patch resolve() failed to get file path from XML");
        assertEquals("13.9.4.2.5", patchFile.getVersion(), "wrong version selected");
        String filePathFromCache = cacheStore.getValueFromCache("28186730_13.9.4.2.5");
        assertNotNull(filePathFromCache, "Could not find new patch in cache");
        assertEquals(filePath, filePathFromCache, "Patch in cache does not match");
    }
}
