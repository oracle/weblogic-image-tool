// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import org.junit.jupiter.api.AfterAll;
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
                                               String password) throws XPathExpressionException, AruException {
            Document result;
            try {
                // these release numbers are fake test data from the fake releases.xml found in test/resources
                if (releaseNumber.equals("336") || releaseNumber.equals("304")) {
                    result = getResource("/recommended-patches.xml");
                } else {
                    result = getResource("/no-patches.xml");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("failed to load resources XML", e);
            }
            verifyResponse(result);
            return result;
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
}