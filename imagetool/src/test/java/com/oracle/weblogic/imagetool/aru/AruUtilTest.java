// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AruUtilTest {
    private static LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);
        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new TestAruUtil());
    }

    @AfterAll
    static void tearDown() {
        logger.setLevel(oldLevel);
    }

    @AfterEach
    void clearStatic() throws Exception {
        Field documentField = AruUtil.class.getDeclaredField("allReleasesDocument");
        documentField.setAccessible(true);
        documentField.set(null, null);
    }

    /**
     * Intercept calls to the ARU REST API during unit testing.
     */
    public static class TestAruUtil extends AruUtil {
        public TestAruUtil() {
        }

        @Override
        Document getAllReleases(String userId, String password) {
            try {
                return getResource("/releases.xml");
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("failed to load releases.xml from resources", e);
            }
        }

        @Override
        Document getRecommendedPatchesMetadata(AruProduct product, String releaseNumber, String userId,
                                               String password) {
            try {
                if (releaseNumber.equals("336")) {
                    return getResource("/recommended-patches.xml");
                } else {
                    return getResource("/no-patches.xml");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("failed to load resources XML", e);
            }
        }

        private Document getResource(String path) throws IOException {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream(path)))) {
                String doc = buffer.lines().collect(Collectors.joining("\n"));
                return HttpUtil.parseXmlString(doc);
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
}