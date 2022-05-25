// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.aru.InvalidPatchNumberException;
import com.oracle.weblogic.imagetool.aru.MultiplePatchVersionsException;
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
                list.add(new AruPatch().patchId("1").product("15991").description("psu")
                    .psuBundle("Oracle WebLogic Server 12.2.1.0.170418"));
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

        @Override
        public List<AruPatch> getPatches(String bugid, String user, String password)
            throws IOException, XPathExpressionException {
            return AruPatch.getPatches(
                ResourceUtils.getXmlFromResource("/patches/patch-" + bugid + ".xml"));
        }

        public static void insertTestAruInstance() throws NoSuchFieldException, IllegalAccessException {
            // insert test class into AruUtil to intercept REST calls to ARU
            Field aruRest = AruUtil.class.getDeclaredField("instance");
            aruRest.setAccessible(true);
            aruRest.set(aruRest, new CommonPatchingOptionsTest.TestAruUtil());
        }

        public static void removeTestAruInstance() throws NoSuchFieldException, IllegalAccessException {
            // remove test class from AruUtil instance
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
    void withPassword() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy");

        assertFalse(createImage.applyingPatches(), "No patches on the command line, but applyingPatches is true");
    }

    @Test
    void invalidPatchId() {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "abc");

        assertThrows(InvalidPatchIdFormatException.class, createImage::initializeOptions);
    }

    @Test
    void invalidPatchIdTooFewDigits() {
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
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
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

    @Test
    void findPsuTest() throws AruException, IOException, InvalidPatchIdFormatException {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--recommendedPatches");
        createImage.initializeOptions();
        List<AruPatch> aruPatches = createImage.getRecommendedPatchList();
        String psuVersion = createImage.findPsuVersion(aruPatches, null);
        assertEquals("12.2.1.0.170418", psuVersion);
    }

    @Test
    void findPsu2Test() throws AruException, IOException, InvalidPatchIdFormatException {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--type", "FMW", "--recommendedPatches");
        createImage.initializeOptions();
        List<AruPatch> aruPatches = createImage.getRecommendedPatchList();
        String psuVersion = createImage.findPsuVersion(aruPatches, "1234");
        // TestAruUtil returns no PSUs for type FMW, default should be returned
        assertEquals("1234", psuVersion);
    }

    @Test
    void resolveUserRequestedPatchesTest()
        throws InvalidCredentialException, IOException, InvalidPatchIdFormatException {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "11100004");
        createImage.initializeOptions();
        // User does not select a patch version and no PSU is present
        assertThrows(MultiplePatchVersionsException.class, () -> createImage.resolveUserRequestedPatches(null));
    }

    @Test
    void resolveUserRequestedPatches2Test()
        throws AruException, IOException, InvalidPatchIdFormatException, XPathExpressionException {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "11100004");
        createImage.initializeOptions();
        // User does not select a patch version and a PSU is already installed
        List<AruPatch> results = createImage.resolveUserRequestedPatches("12.2.1.4.220105");
        assertEquals(1, results.size());
        assertEquals("11100004", results.get(0).patchId());
        assertEquals("12.2.1.4.220105", results.get(0).version());
    }

    @Test
    void resolveUserRequestedPatches3Test()
        throws AruException, IOException, InvalidPatchIdFormatException, XPathExpressionException {
        CreateImage createImage = new CreateImage();
        // user specifies OPatch bug number and normal patch.  OPatch bug number should be discarded.
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "28186730,11100004_12.2.1.3.211222");
        createImage.initializeOptions();
        // User does select a patch version and a PSU is already installed
        List<AruPatch> results = createImage.resolveUserRequestedPatches("12.1.3.0.220118");
        assertEquals(1, results.size());
        assertEquals("11100004", results.get(0).patchId());
        assertEquals("12.2.1.3.211222", results.get(0).version());
    }

    @Test
    void resolveUserRequestedPatches4Test()
        throws InvalidCredentialException, IOException, InvalidPatchIdFormatException {
        CreateImage createImage = new CreateImage();
        // 11100005 is a Stack Patch Bundle patch
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "11100004_12.1.3.0.220118,11100005");
        createImage.initializeOptions();
        // User specified a Stack Patch Bundle patch
        assertThrows(InvalidPatchNumberException.class, () -> createImage.resolveUserRequestedPatches(null));
    }

    @Test
    void resolveUserRequestedPatches5Test()
        throws AruException, IOException, InvalidPatchIdFormatException, XPathExpressionException {
        CreateImage createImage = new CreateImage();
        // 11100006 is a PSU, and 11100004 has a matching version to that PSU that should be selected
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "xxxx", "--password", "yyyy",
            "--patches", "11100006,11100004");
        createImage.initializeOptions();
        // User specified a Stack Patch Bundle patch
        List<AruPatch> results = createImage.resolveUserRequestedPatches(null);
        assertEquals(2, results.size());
        assertEquals("11100004", results.get(1).patchId());
        assertEquals("12.2.1.3.211222", results.get(1).version());
    }
}
