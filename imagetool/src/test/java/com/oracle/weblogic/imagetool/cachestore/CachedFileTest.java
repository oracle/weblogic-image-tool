// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.oracle.weblogic.imagetool.cachestore.OPatchFile.DEFAULT_BUG_NUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@ReduceTestLogging(loggerClass = CachedFile.class)
class CachedFileTest {

    static Path cacheDir;
    static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");
    static final String VER_12213 = "12.2.1.3.0";

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir) throws IOException {
        Path path12213 = tempDir.resolve("installer.file.122130.jar");
        Files.write(path12213, fileContents);
        Path path12214 = tempDir.resolve("installer.file.12214.jar");
        Files.write(path12214, fileContents);
        Path path1411 = tempDir.resolve("installer.file.141100.jar");
        Files.write(path1411, fileContents);

        Path settingsFileName = tempDir.resolve("settings.yaml");
        Path installerFile = tempDir.resolve("installers.yaml");
        Path patchFile = tempDir.resolve("patches.yaml");
        Files.createFile(settingsFileName);
        Files.createFile(installerFile);
        Files.createFile(patchFile);


        List<String> lines = Arrays.asList(
            "installerSettingsFile: " + installerFile.toAbsolutePath().toString(),
            "patchSettingsFile: " + patchFile.toAbsolutePath().toString(),
            "installerDirectory: " + tempDir.toAbsolutePath().toString(),
            "patchDirectory: " + tempDir.toAbsolutePath().toString()
        );
        Files.write(settingsFileName, lines);
        ConfigManager configManager = ConfigManager.getInstance(settingsFileName);


        InstallerMetaData installer1 = new InstallerMetaData("Generic", path12213.toString(),
            VER_12213);
        InstallerMetaData installer2 = new InstallerMetaData(Architecture.getLocalArchitecture().toString(),
            path12214.toString(),
            "12.2.1.4.0");
        InstallerMetaData installer3 = new InstallerMetaData("linux/amd64", path1411.toString(),
            "14.1.1.0.0");
        InstallerMetaData installer4 = new InstallerMetaData("linux/arm64", path1411.toString(),
            "14.1.1.0.0");

        configManager.addInstaller(InstallerType.WLS, VER_12213, installer1);
        configManager.addInstaller(InstallerType.WLS, "12.2.1.4.0", installer2);
        configManager.addInstaller(InstallerType.WLS, "14.1.1.0.0", installer3);
        configManager.addInstaller(InstallerType.WLS, "14.1.1.0.0", installer4);

        addPatchesToLocal(tempDir, configManager, patchFile, DEFAULT_BUG_NUM,
            "Generic", "patch1.zip", "13.9.2.0.0");
        addPatchesToLocal(tempDir, configManager, patchFile, DEFAULT_BUG_NUM,
            "Generic", "patch1.zip", "13.9.4.0.0");
        addPatchesToLocal(tempDir, configManager, patchFile, DEFAULT_BUG_NUM,
            "Generic", "patch1.zip", "13.9.2.2.2");

        // OPatch files
        //cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.0.0", "/not/used");
        //cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.4.0.0", "/not/used");
        //cacheStore.addToCache(DEFAULT_BUG_NUM + "_13.9.2.2.2", "/not/used");
    }

    private static void addPatchesToLocal(Path tempDir, ConfigManager configManager, Path patchListingFile,
                                          String bugNumber, String patchArchitecture, String patchLocation,
                                          String patchVersion) throws IOException {
        Map<String, List<PatchMetaData>> patches = configManager.getAllPatches();
        List<PatchMetaData> latestPatches = patches.get(bugNumber);
        if (latestPatches == null) {
            latestPatches = new ArrayList<>();
        }
        Path path = tempDir.resolve(patchLocation);
        Files.write(path, fileContents);
        PatchMetaData latestPatch = new PatchMetaData(patchArchitecture, path.toAbsolutePath().toString(),
            Utils.getSha256Hash(path.toAbsolutePath().toString()),"2024-10-17", patchVersion);
        latestPatches.add(latestPatch);
        patches.put(bugNumber, latestPatches);
        configManager.saveAllPatches(patches, patchListingFile.toAbsolutePath().toString());
    }

    @Test
    void versionString() {
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, "12.2.1.3.0");
        assertEquals("12.2.1.3.0", wlsInstallerFile.getVersion(),
            "CachedFile should return the version from the constructor");
    }

    ////@Test
    //void userProvidedPatchVersionAsId() {
    //    // User provided a patch ID with the version string in the ID
    //    CachedFile cf = new CachedFile("something_versionString", "12.2.1.2.0", Architecture.AMD64);
    //    // if the patch ID has the version, CachedFile should ignore the installer version passed to the constructor,
    //    // and use the version in the ID.
    //    assertEquals("something_versionString", cf.getPatchId(),
    //        "CachedFile getKey() failed when version string was provided by the user in the ID");
    //
    //    // getVersion should always return the version that was provided in the constructor, not the one in the ID
    //    assertEquals("12.2.1.2.0", cf.getVersion(), "CachedFile returned wrong version");
    //}

    @Test
    void resolveFileNotFound() {
        // resolve should fail for a CachedFile that is not in the store
        CachedFile fakeFile = new CachedFile(InstallerType.WLS, "10.3.6.0.0");
        assertThrows(FileNotFoundException.class, () -> fakeFile.resolve());
    }

    @Test
    void resolveFileFindsFile() throws IOException {
        // Resolve a CachedFile stored in the cache (created in test setup above)
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, VER_12213);
        InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.GENERIC, VER_12213);
        String expected = metaData.getLocation();
        assertEquals(expected, wlsInstallerFile.resolve(), "CachedFile did not resolve file");
    }

    @Test
    void resolveNoArchFile() throws IOException {
        // Look for a cache entry where the user specified the architecture/platform amd64
        CachedFile wlsNoArch = new CachedFile(InstallerType.WLS, VER_12213, Architecture.getLocalArchitecture());
        InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.AMD64, VER_12213);

        // verify the cache is setup as expected.
        // wls_12.2.1.3.0 is in the cache, but wls_12.2.1.3.0_amd64 is NOT in the cache
        assertNull(metaData);
        metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.GENERIC, VER_12213);
        String expected = metaData.getLocation();
        assertEquals(expected, wlsNoArch.resolve(), "CachedFile returned wrong file");
    }

    @Test
    void resolveWithArchitecture() throws IOException {
        // Look for a cache entry where the user specified the architecture/platform amd64
        CachedFile wlsArch = new CachedFile(InstallerType.WLS, "14.1.1.0.0", Architecture.fromString("amd64"));
        InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.AMD64, "14.1.1.0.0");

        // verify the cache is setup as expected.  wls_14.1.1.0.0_amd64 is in the cache
        String expected = metaData.getLocation();
        assertNotNull(expected);

        assertEquals(expected, wlsArch.resolve(), "CachedFile failed to find specific architecture");
    }

    @Test
    void resolveFallbackToLocalArch() throws IOException {
        // Look for a cache entry where the user did not specify the architecture/platform
        CachedFile wlsArch = new CachedFile(InstallerType.WLS, "12.2.1.4.0");
        InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.GENERIC, "12.2.1.4.0");

        // verify the cache is setup as expected.  wls_14.1.1.0.0_amd64 is in the cache, but wls_14.1.1.0.0 is not
        assertNull(metaData);
        metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.WLS,
            Architecture.getLocalArchitecture(), "12.2.1.4.0");
        String expected = metaData.getLocation();
        //cacheStore.getValueFromCache("wls_12.2.1.4.0_" + Architecture.getLocalArchitecture());
        assertNotNull(expected);

        assertEquals(expected, wlsArch.resolve(), "CachedFile failed to check local architecture");
    }

    @Test
    void copyFile(@TempDir Path contextDir) throws Exception {
        CachedFile wlsInstallerFile = new CachedFile(InstallerType.WLS, VER_12213);
        // copy the file from the cache store to the fake build context directory
        Path result = wlsInstallerFile.copyFile(contextDir.toString());
        // check to see if the file was copied correctly by examining the contents of the resulting file
        assertLinesMatch(fileContents, Files.readAllLines(result),
            "copied file contents do not match source");
    }

    @Test
    void latestOpatchVersion() throws IOException, AruException, XPathExpressionException {
        // OPatch file should default to the default OPatch bug number and the latest version found in cache
        OPatchFile patchFile = OPatchFile.getInstance(null, null, null);
        assertEquals("13.9.4.0.0", patchFile.getVersion(), "wrong version selected");

        //assertEquals(DEFAULT_BUG_NUM + "_13.9.4.0.0", patchFile.getPatchId(),
        //    "failed to get latest Opatch version from the cache");
    }

    @Test
    void specificOpatchVersion() throws IOException, AruException, XPathExpressionException {
        // OPatch file should default to the default OPatch bug number and the latest version found in cache
        OPatchFile patchFile = OPatchFile.getInstance(DEFAULT_BUG_NUM, "13.9.2.2.2",
            null, null);
        assertEquals("13.9.2.2.2", patchFile.getVersion(),
            "failed to get specific Opatch version from the cache");
    }
}
