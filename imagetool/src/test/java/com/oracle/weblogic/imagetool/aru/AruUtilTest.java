// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AruUtilTest {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);
        // insert test class into AruUtil to intercept REST calls to ARU
        MockAruUtil.insertMockAruInstance(new TestAruUtil());
    }

    @AfterAll
    static void tearDown() throws NoSuchFieldException, IllegalAccessException {
        logger.setLevel(oldLevel);
        MockAruUtil.removeMockAruInstance();
    }

    /**
     * Intercept calls to the ARU REST API during unit testing.
     */
    public static class TestAruUtil extends MockAruUtil {
        @Override
        Document getRecommendedPatchesMetadata(AruProduct product, String releaseNumber, String userId,
                                               String password) {
            Document result;
            try {
                // these release numbers are fake test data from the fake releases.xml found in test/resources
                if (releaseNumber.equals("336") || releaseNumber.equals("304")) {
                    result = ResourceUtils.getXmlFromResource("/patches/recommended-patches.xml");
                } else {
                    result = ResourceUtils.getXmlFromResource("/patches/no-patches.xml");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("failed to load resources XML", e);
            }
            return result;
        }

        @Override
        Document patchConflictCheck(String payload, String userId, String password) throws IOException {
            if (payload.contains("<patch_group rel_id")) {
                return ResourceUtils.getXmlFromResource("/conflict-check/double-conflict.xml");
            } else {
                return ResourceUtils.getXmlFromResource("/conflict-check/no-conflict.xml");
            }
        }
    }

    @Test
    void testRecommendedPatches() throws Exception {
        List<AruPatch> recommendedPatches =
            AruUtil.rest().getRecommendedPatches(AruProduct.WLS, "12.2.1.3.0", "x", "x");
        assertEquals(5, recommendedPatches.size());
        List<String> bugs = recommendedPatches.stream().map(AruPatch::patchId).collect(Collectors.toList());
        assertTrue(bugs.contains("31544340"));
        assertTrue(bugs.contains("31535411"));
        assertTrue(bugs.contains("31384951"));
        assertTrue(bugs.contains("28512225"));
        assertTrue(bugs.contains("28278427"));

        // if no recommended patches are found, method should return an empty list (test data does not have 12.2.1.4)
        recommendedPatches =
            AruUtil.rest().getRecommendedPatches(FmwInstallerType.WLS, "12.2.1.4.0", "x", "x");
        assertTrue(recommendedPatches.isEmpty());
    }

    @Test
    void testNoRecommendedPatches() throws Exception {
        List<AruPatch> recommendedPatches =
            AruUtil.rest().getRecommendedPatches(AruProduct.WLS, "12.2.1.4.0", "x", "x");
        assertEquals(0, recommendedPatches.size());
    }

    @Test
    void testGetLatestPsu() throws Exception {
        List<AruPatch> latestPsu =
            AruUtil.rest().getLatestPsu(AruProduct.WLS, "12.2.1.3.0", "x", "x");
        assertEquals(1, latestPsu.size());
        List<String> bugs = latestPsu.stream().map(AruPatch::patchId).collect(Collectors.toList());
        assertTrue(bugs.contains("31535411"));
    }

    @Test
    void testReleaseNotFound() throws Exception {
        // should not throw an exception, and return no patches when release does not exist
        List<AruPatch> latestPsu =
            AruUtil.rest().getLatestPsu(AruProduct.WLS, "3.0.0.0.0", "x", "x");
        assertEquals(0, latestPsu.size());

        List<AruPatch> recommendedPatches =
            AruUtil.rest().getRecommendedPatches(AruProduct.WLS, "3.0.0.0.0", "x", "x");
        assertEquals(0, recommendedPatches.size());
    }

    @Test
    void testPatchConflictSets() throws IOException {
        Document value = ResourceUtils.getXmlFromResource("/conflict-check/simple-conflict.xml");
        List<List<String>> conflicts = AruUtil.rest().getPatchConflictSets(value);
        assertEquals(1, conflicts.size());
    }

    @Test
    void testPatchConflictSets2() throws IOException {

        Document value = ResourceUtils.getXmlFromResource("/conflict-check/double-conflict.xml");
        List<List<String>> conflicts = AruUtil.rest().getPatchConflictSets(value);
        assertEquals(2, conflicts.size());
    }

    @Test
    void testValidatePatches() throws IOException, AruException {
        // should not throw a conflict exception
        AruUtil.rest().validatePatches(new ArrayList<>(), new ArrayList<>(), "blah", "blah");

        List<AruPatch> patches = new ArrayList<>();
        patches.add(new AruPatch());
        assertThrows(PatchConflictException.class,
            () -> AruUtil.rest().validatePatches(new ArrayList<>(), patches, "blah", "blah"));
    }

}