// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
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
    static void setUp() {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);
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
                                                    String userId, String password) throws AruException {
            List<AruPatch> list = new ArrayList<>();
            list.add(new AruPatch().patchId("1").description("psu").psuBundle("x"));
            list.add(new AruPatch().patchId("2").description("blah"));
            list.add(new AruPatch().patchId("3").description("ADR FOR WEBLOGIC SERVER"));
            return list;
        }
    }

    @AfterAll
    static void tearDown() {
        logger.setLevel(oldLevel);
    }

    @Test
    void noPassword() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek");
        assertThrows(InvalidCredentialException.class, createImage::initializeOptions);
    }

    @Test
    void withPassword() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        assertFalse(createImage.applyingPatches(), "No patches on the command line, but applyingPatches is true");
    }

    @Test
    void invalidPatchId() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "abc");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        assertThrows(InvalidPatchIdFormatException.class, createImage::initializeOptions);
    }

    @Test
    void invalidPatchIdTooFewDigits() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "1234567");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        assertThrows(InvalidPatchIdFormatException.class, createImage::initializeOptions);
    }

    @Test
    void validPatchId() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        createImage.initializeOptions();
        assertTrue(createImage.applyingPatches(), "User provided patches but applyingPatches still false");
    }

    @Test
    void doNotGetRecommendedPatches() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        List<AruPatch> patches = createImage.getRecommendedPatchList(FmwInstallerType.WLS);
        assertTrue(patches.isEmpty(), "Neither latestPSU nor recommendedPatches was specified, but returned patches");
    }

    @Test
    void getRecommendedPatches() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "12345678", "--recommendedPatches");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        createImage.initializeOptions();
        List<AruPatch> patches = createImage.getRecommendedPatchList(FmwInstallerType.WLS);
        assertFalse(patches.isEmpty(), "recommendedPatches was specified, but returned patches was empty");
        assertEquals(2, patches.size(),"ADR patch was not removed?");
    }

    @Test
    void getRecommendedPatchesWithoutCredentials() throws Exception {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1",
            "--patches", "12345678", "--recommendedPatches");

        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());

        createImage.initializeOptions();
        assertThrows(IllegalArgumentException.class, () -> createImage.getRecommendedPatchList(FmwInstallerType.WLS));
    }

}
