// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.MockAruUtil;
import com.oracle.weblogic.imagetool.cli.menu.CommonOptions;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import com.oracle.weblogic.imagetool.util.TestSetup;
import com.oracle.weblogic.imagetool.util.Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ReduceTestLogging(loggerClass = CommonOptions.class)
@Tag("unit")
class OPatchFileTest {
    private static CacheStore cacheStore;
    static final List<String> fileContents = Arrays.asList("A", "B", "C");

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir)
        throws NoSuchFieldException, IllegalAccessException, IOException {

        // Mock REST calls to ARU for patches
        MockAruUtil.insertMockAruInstance(new MockAruUtil());

        TestSetup.setup(tempDir);
        ConfigManager configManager = ConfigManager.getInstance();
        Path patchFile = Paths.get(configManager.getPatchDetailsFile());

        addPatchesToLocal(tempDir, configManager, patchFile, "28186730",
            "Generic", "patch1.zip","13.9.4.2.10");

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
            Utils.getSha256Hash(path.toAbsolutePath().toString()),"2024-10-17", patchVersion, "");
        latestPatches.add(latestPatch);
        patches.put(bugNumber, latestPatches);
        configManager.saveAllPatches(patches, patchListingFile.toAbsolutePath().toString());
    }

    @AfterAll
    static void teardown() throws NoSuchFieldException, IllegalAccessException {
        MockAruUtil.removeMockAruInstance();
    }

    @Test
    void opatchPatchIdTest() {
        assertTrue(OPatchFile.isOPatchPatch("28186730"), "OPatch bug number");
        assertTrue(OPatchFile.isOPatchPatch("28186730_13.9.4.2.4"), "OPatch bug number with version");
        assertTrue(OPatchFile.isOPatchPatch("28186730_1"), "OPatch bug number with separator");

        assertFalse(OPatchFile.isOPatchPatch("28186731"), "Should not match");
        assertFalse(OPatchFile.isOPatchPatch("281867301"), "OPatch bug number with additional number");
    }

    private void checkOpatchVersion(String expectedVersion, String patchId)
        throws XPathExpressionException, IOException, AruException {

        OPatchFile opatchFile = OPatchFile.getInstance(patchId, "xxxx", "yyyy");
        assertNotNull(opatchFile);
        assertEquals(expectedVersion, opatchFile.getVersion());
    }

    @Test
    void onlineFindSpecifiedVersion() throws XPathExpressionException, IOException, AruException {
        // if the user specifies the opatchBugNumber with a specific version, the code should search ARU
        // and if not found, find it locally in the cache
        checkOpatchVersion("13.9.4.2.10", "28186730_13.9.4.2.10");

        // if the user specifies the opatchBugNumber with a specific version, the code should search ARU
        // and if found, return the one that was found   13.9.4.2.5
        checkOpatchVersion("13.9.4.2.5", "28186730_13.9.4.2.5");
    }

    @Test
    void onlineFindAruVersion() throws XPathExpressionException, IOException, AruException {
        // if the user provides just the OPatch bug number, the code should search ARU for the latest available version
        checkOpatchVersion("13.9.4.2.5", "28186730");

        // if the user does not specify an opatchBugNumber, the code should search ARU for the latest available version
        checkOpatchVersion("13.9.4.2.5", null);
    }

    @Test
    void offlineFindHighestVersion() throws XPathExpressionException, IOException, AruException {
        // if the user does not specify an opatchBugNumber,
        // the code should search the local cache for the highest version
        OPatchFile opatchFile = OPatchFile.getInstance(null, null, null);
        assertNotNull(opatchFile);
        assertEquals("13.9.4.2.10", opatchFile.getVersion());
    }
}
