// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.MockAruUtil;
import com.oracle.weblogic.imagetool.cli.menu.CommonOptions;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
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

    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path cacheDir)
        throws NoSuchFieldException, IllegalAccessException, IOException {

        // Mock REST calls to ARU for patches
        MockAruUtil.insertMockAruInstance(new MockAruUtil());

        // Create a fake cache with a fake OPatch install
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        Path installer = tempDir.resolve("opatch_install.zip");
        Files.write(installer, Arrays.asList("A", "B", "C"));
        cacheStore.addToCache("28186730_13.9.4.2.10", installer.toString());
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
