// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.InvalidPatchIdFormatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CommonPatchingOptionsTest {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);
        TestAruUtil.insertTestAruInstance();
    }

    public static class TestAruUtil extends AruUtil {

        public TestAruUtil() {
        }

        @Override
        public boolean checkCredentials(String username, String password) {
            return true;
        }

        @Override
        public List<AruPatch> getRecommendedPatches(FmwInstallerType type, String version,
                                                    String userId, String password) {
            if (type.equals(FmwInstallerType.WLS)) {
                List<AruPatch> list = new ArrayList<>();
                list.add(new AruPatch().patchId("1").description("psu").psuBundle("x"));
                list.add(new AruPatch().patchId("2").description("blah"));
                list.add(new AruPatch().patchId("3").description("ADR FOR WEBLOGIC SERVER"));
                return list;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<AruPatch> getLatestPsu(FmwInstallerType type, String version, String userId, String password) {
            if (type.equals(FmwInstallerType.WLS)) {
                List<AruPatch> list = new ArrayList<>();
                list.add(new AruPatch().patchId("1").description("psu").psuBundle("x"));
                return list;
            } else {
                return Collections.emptyList();
            }
        }

        public static void insertTestAruInstance() throws NoSuchFieldException, IllegalAccessException {
            // insert test class into AruUtil to intercept REST calls to ARU
            Field aruRest = AruUtil.class.getDeclaredField("instance");
            aruRest.setAccessible(true);
            aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());
        }

        public static void removeTestAruInstance() throws NoSuchFieldException, IllegalAccessException {
            // insert test class into AruUtil to intercept REST calls to ARU
            Field aruRest = AruUtil.class.getDeclaredField("instance");
            aruRest.setAccessible(true);
            aruRest.set(aruRest, null);
        }
    }

    @AfterAll
    static void tearDown() throws NoSuchFieldException, IllegalAccessException {
        logger.setLevel(oldLevel);
        TestAruUtil.removeTestAruInstance();
    }

    @Test
    void withPassword() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx");

        assertFalse(createImage.applyingPatches(), "No patches on the command line, but applyingPatches is true");
    }

    @Test
    void invalidPatchId() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "abc");

        assertThrows(InvalidPatchIdFormatException.class, createImage::initializeOptions);
    }

    @Test
    void invalidPatchIdTooFewDigits() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "1234567");

        assertThrows(InvalidPatchIdFormatException.class, createImage::initializeOptions);
    }

    @Test
    void validPatchId() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678");

        createImage.initializeOptions();
        assertTrue(createImage.applyingPatches(), "User provided patches but applyingPatches still false");
    }

    @Test
    void doNotGetRecommendedPatches() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678");

        List<AruPatch> patches = createImage.getRecommendedPatchList();
        assertTrue(patches.isEmpty(), "Neither latestPSU nor recommendedPatches was specified, but returned patches");
    }

    @Test
    void getLatestPsu() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678", "--latestPSU");

        createImage.initializeOptions();
        List<AruPatch> patches = createImage.getRecommendedPatchList();
        assertFalse(patches.isEmpty(), "recommendedPatches was specified, but returned patches was empty");
        assertEquals(1, patches.size());
    }

    @Test
    void getLatestPsuButEmpty() throws Exception {
        CreateImage createImage = new CreateImage();
        // Using type = FMW with TestAruUtil class injected to verify that latestPSU flag is flipped when no patches
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--type", "FMW",
            "--user", "derek", "--password", "xxx", "--patches", "12345678", "--latestPSU");

        createImage.initializeOptions();
        List<AruPatch> patches = createImage.getRecommendedPatchList();
        assertTrue(patches.isEmpty());
        assertFalse(createImage.applyingRecommendedPatches());
        assertTrue(createImage.applyingPatches());
    }

    @Test
    void getRecommendedPatches() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678", "--recommendedPatches");

        createImage.initializeOptions();
        List<AruPatch> patches = createImage.getRecommendedPatchList();
        assertFalse(patches.isEmpty(), "recommendedPatches was specified, but returned patches was empty");
        assertEquals(2, patches.size(),"ADR patch was not removed?");
    }

    @Test
    void getRecommendedPatchesButEmpty() throws Exception {
        // Using type = FMW with TestAruUtil class injected to verify that recommendedPatches flag is flipped
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--type", "FMW",
            "--user", "derek", "--password", "xxx", "--recommendedPatches");

        createImage.initializeOptions();
        List<AruPatch> patches = createImage.getRecommendedPatchList();
        assertTrue(patches.isEmpty(), "Expected 0 patches for type FMW");
    }

    @Test
    void getRecommendedPatchesWithoutCredentials() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1",
            "--patches", "12345678", "--recommendedPatches");

        createImage.initializeOptions();
        assertThrows(IllegalArgumentException.class, createImage::getRecommendedPatchList);
    }

    @Test
    void typeFlag() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--type", "FMW");
        assertTrue(createImage.isInstallerTypeSet());
        assertEquals(FmwInstallerType.FMW, createImage.getInstallerType());
    }

    @Test
    void applyingSwitchTest1() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1");
        assertEquals(FmwInstallerType.WLS, createImage.getInstallerType());
        assertFalse(createImage.applyingPatches());
        assertFalse(createImage.applyingRecommendedPatches());
    }

    @Test
    void applyingSwitchTest2() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--recommendedPatches");
        assertEquals(FmwInstallerType.WLS, createImage.getInstallerType());
        assertTrue(createImage.applyingPatches());
        assertTrue(createImage.applyingRecommendedPatches());
    }
}
