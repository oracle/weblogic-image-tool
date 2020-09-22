// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.tests.annotations.IntegrationTest;
import com.oracle.weblogic.imagetool.tests.annotations.Logger;
import com.oracle.weblogic.imagetool.tests.utils.CacheCommand;
import com.oracle.weblogic.imagetool.tests.utils.CommandResult;
import com.oracle.weblogic.imagetool.tests.utils.CreateCommand;
import com.oracle.weblogic.imagetool.tests.utils.RebaseCommand;
import com.oracle.weblogic.imagetool.tests.utils.Runner;
import com.oracle.weblogic.imagetool.tests.utils.UpdateCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@IntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ITImagetool extends BaseTest {

    @Logger
    private static final LoggingFacade logger = LoggingFactory.getLogger(ITImagetool.class);

    private static final String JDK_INSTALLER = "jdk-8u202-linux-x64.tar.gz";
    private static final String JDK_INSTALLER_NEWER = "jdk-8u231-linux-x64.tar.gz";
    private static final String WLS_INSTALLER = "fmw_12.2.1.3.0_wls_Disk1_1of1.zip";
    private static final String P27342434_INSTALLER = "p27342434_122130_Generic.zip";
    private static final String P28186730_INSTALLER = "p28186730_139422_Generic.zip";
    private static final String P22987840_INSTALLER = "p22987840_122100_Generic.zip";
    private static final String WDT_INSTALLER = "weblogic-deploy.zip";
    private static final String FMW_INSTALLER = "fmw_12.2.1.3.0_infrastructure_Disk1_1of1.zip";
    private static final String TEST_ENTRY_KEY = "mytestEntryKey";
    private static final String P27342434_ID = "27342434";
    private static final String P28186730_ID = "28186730";
    private static final String P22987840_ID = "22987840";
    private static final String WLS_VERSION = "12.2.1.3.0";
    private static final String OPATCH_VERSION = "13.9.4.2.2";
    private static final String JDK_VERSION = "8u202";
    private static final String JDK_VERSION_8u212 = "8u212";
    private static final String WDT_VERSION = "1.1.2";
    private static final Path WDT_ARCHIVE = Paths.get("target", "wdt", "archive.zip");
    private static final Path WDT_RESOURCES = Paths.get("src", "test", "resources", "wdt");
    private static final Path WDT_VARIABLES = WDT_RESOURCES.resolve("domain.properties");
    private static final Path WDT_MODEL = WDT_RESOURCES.resolve("simple-topology.yaml");
    private static final Path WDT_MODEL2 = WDT_RESOURCES.resolve("simple-topology2.yaml");
    private static String oracleSupportUsername;
    private static boolean wlsImgBuilt = false;
    private static boolean domainImgBuilt = false;

    @BeforeAll
    static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");
        // verify that all the prerequisites are set and exist
        validateEnvironmentSettings();
        // clean up Docker instances leftover from a previous run
        cleanup();

        setup();
        logger.info("Pulling OS base images from OCIR ...");
        pullDockerImage(BASE_OS_IMG, BASE_OS_IMG_TAG);

        logger.info("Pulling Oracle DB image from OCIR ...");
        pullDockerImage(ORACLE_DB_IMG, ORACLE_DB_IMG_TAG);

        // verify that required files/installers are available
        verifyStagedFiles(JDK_INSTALLER, WLS_INSTALLER, WDT_INSTALLER, P27342434_INSTALLER, P28186730_INSTALLER,
            FMW_INSTALLER, JDK_INSTALLER_NEWER, P22987840_INSTALLER);

        // get Oracle support credentials
        oracleSupportUsername = System.getenv("ORACLE_SUPPORT_USERNAME");
        String oracleSupportPassword = System.getenv("ORACLE_SUPPORT_PASSWORD");
        if (oracleSupportUsername == null || oracleSupportPassword == null) {
            throw new Exception("Please set environment variables ORACLE_SUPPORT_USERNAME and ORACLE_SUPPORT_PASSWORD"
                + " for Oracle Support credentials to download the patches.");
        }
    }

    @AfterAll
    static void staticUnprepare() throws Exception {
        logger.info("cleaning up after the test ...");
        cleanup();
    }

    /**
     * Create the log directory in ./target (build folder), and open a new file using the test method's name.
     * @param testInfo metadata from the test to be logged
     * @return an output file wrapped in a PrintWriter
     * @throws IOException if the PrintWriter fails to open the file
     */
    private static PrintWriter getTestMethodWriter(TestInfo testInfo) throws IOException {
        if (testInfo.getTestMethod().isPresent()) {
            String methodName = testInfo.getTestMethod().get().getName();
            // create a output file in the build folder with the name {test method name}.stdout
            Path outputPath = Paths.get("target", "logs", methodName + ".out");
            Files.createDirectories(outputPath.getParent());
            logger.info("Test log: {0}", outputPath.toString());
            return new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath.toString()))), true);
        } else {
            throw new IllegalArgumentException("Method is not present in this context, and this method cannot be used");
        }
    }

    private static String getMethodName(TestInfo testInfo) {
        if (testInfo.getTestMethod().isPresent()) {
            return testInfo.getTestMethod().get().getName();
        } else {
            throw new IllegalArgumentException("Cannot call getMethodName outside of test method");
        }

    }

    private static void pullDockerImage(String imagename, String imagetag) throws Exception {

        String pullCommand = "docker pull " + imagename + ":" + imagetag;
        logger.info(pullCommand);
        Runner.run(pullCommand);

        // verify the docker image is pulled
        CommandResult result = Runner.run("docker images | grep " + imagename  + " | grep "
            + imagetag + "| wc -l");
        String resultString = result.stdout();
        if (Integer.parseInt(resultString.trim()) != 1) {
            throw new Exception("docker image " + imagename + ":" + imagetag + " is not pulled as expected."
                + " Expected 1 image, found " + resultString);
        }
    }

    /**
     * Test caching of an installer of type JDK.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(1)
    @Tag("gate")
    @Tag("cache")
    @DisplayName("Add JDK installer to cache")
    void cacheAddInstallerJdk(TestInfo testInfo) throws Exception {
        String jdkPath = STAGING_DIR + FS + JDK_INSTALLER;
        String command = new CacheCommand()
            .addInstaller(true)
            .type("jdk")
            .version(JDK_VERSION)
            .path(jdkPath)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should show newly added JDK installer
            assertTrue(listResult.stdout().contains("jdk_" + JDK_VERSION + "=" + jdkPath));
        }
    }

    /**
     * Test caching of an installer of type WLS.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(2)
    @Tag("gate")
    @Tag("cache")
    @DisplayName("Add WLS installer to cache")
    void cacheAddInstallerWls(TestInfo testInfo) throws Exception {
        String wlsPath = STAGING_DIR + FS + WLS_INSTALLER;
        String command = new CacheCommand()
            .addInstaller(true)
            .type("wls")
            .version(WLS_VERSION)
            .path(wlsPath)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should show newly added WLS installer
            assertTrue(listResult.stdout().contains("wls_" + WLS_VERSION + "=" + wlsPath));
        }
    }

    /**
     * Test manual caching of a patch JAR.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(3)
    @Tag("gate")
    @Tag("cache")
    @DisplayName("Add patch 27342434 to cache")
    void cacheAddPatch(TestInfo testInfo) throws Exception {
        String patchPath = STAGING_DIR + FS + P27342434_INSTALLER;
        String command = new CacheCommand()
            .addPatch(true)
            .path(patchPath)
            .patchId(P27342434_ID, WLS_VERSION)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should show newly added patch
            assertTrue(listResult.stdout().contains(P27342434_ID + "_" + WLS_VERSION + "=" + patchPath));
        }
    }

    /**
     * add an entry to the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(4)
    @Tag("gate")
    @DisplayName("Add manual entry to cache")
    void cacheAddTestEntry(TestInfo testInfo) throws Exception {
        String testEntryValue = STAGING_DIR + FS + P27342434_INSTALLER;
        String command = new CacheCommand()
            .addEntry(true)
            .key(TEST_ENTRY_KEY)
            .value(testEntryValue)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult addEntryResult = Runner.run(command, out, logger);
            assertEquals(0, addEntryResult.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should show newly added patch
            assertTrue(listResult.stdout().contains(TEST_ENTRY_KEY.toLowerCase() + "=" + testEntryValue));
            // cache should also contain the installer that was added in the previous test (persistent cache)
            assertTrue(listResult.stdout().contains(P27342434_ID + "_" + WLS_VERSION + "="));
        }
    }

    /**
     * test delete an entry from the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(5)
    @Tag("gate")
    @DisplayName("Delete cache entry")
    void cacheDeleteTestEntry(TestInfo testInfo) throws Exception {
        String command = new CacheCommand()
            .deleteEntry(true)
            .key(TEST_ENTRY_KEY)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should NOT show deleted patch
            assertFalse(listResult.stdout().contains(TEST_ENTRY_KEY.toLowerCase()));
        }
    }

    /**
     * Test manual caching of a patch JAR.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(6)
    @Tag("gate")
    @Tag("cache")
    @DisplayName("Add OPatch patch to cache")
    void cacheOpatch(TestInfo testInfo) throws Exception {
        String patchPath = STAGING_DIR + FS + P28186730_INSTALLER;
        String command = new CacheCommand()
            .addPatch(true)
            .path(patchPath)
            .patchId(P28186730_ID, OPATCH_VERSION)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the result
            String listCommand = new CacheCommand().listItems(true).build();
            CommandResult listResult = Runner.run(listCommand, out, logger);
            // the process return code for listItems should be 0
            assertEquals(0, listResult.exitValue(), "for command: " + listCommand);
            // output should show newly added patch
            assertTrue(listResult.stdout().contains(P28186730_ID + "_" + OPATCH_VERSION + "=" + patchPath));
        }
    }

    /**
     * create a WLS image with default WLS version.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(10)
    @Tag("gate")
    @DisplayName("Create default WebLogic Server image")
    void createWlsImg(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand().tag(tagName).build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
            wlsImgBuilt = true;
        }
    }

    /**
     * update a WLS image with a patch.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(11)
    @Tag("gate")
    @DisplayName("Update createWlsImg with patch 27342434")
    void updateWlsImg(TestInfo testInfo) throws Exception {
        assumeTrue(wlsImgBuilt);

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new UpdateCommand()
            .fromImage(build_tag + ":createWlsImg")
            .tag(tagName)
            .patches(P27342434_ID)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
            // TODO should check that patch and OPatch were applied
        }
    }

    /**
     * create a WLS image using WebLogic Deploying Tool.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(12)
    @Tag("gate")
    @DisplayName("Create WLS image with WDT domain")
    void createWlsImgUsingWdt(TestInfo testInfo) throws Exception {
        // add WDT installer to the cache
        String wdtPath = STAGING_DIR + FS + WDT_INSTALLER;
        String addCommand = new CacheCommand()
            .addInstaller(true)
            .type("wdt")
            .version(WDT_VERSION)
            .path(wdtPath)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult addResult = Runner.run(addCommand, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, addResult.exitValue(), "for command: " + addCommand);

            // build the wdt archive
            buildWdtArchive();

            String tagName = build_tag + ":" + getMethodName(testInfo);
            // create a WLS image with a domain
            String command = new CreateCommand()
                .fromImage(BASE_OS_IMG, BASE_OS_IMG_TAG)
                .tag(tagName)
                .patches(P27342434_ID)
                .wdtVersion(WDT_VERSION)
                .wdtModel(WDT_MODEL)
                .wdtArchive(WDT_ARCHIVE)
                .wdtDomainHome("/u01/domains/simple_domain")
                .wdtVariables(WDT_VARIABLES)
                .build();

            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
            domainImgBuilt = true;
        }
    }

    /**
     * Use the Rebase function to move a domain to a new image.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(13)
    @Tag("gate")
    @DisplayName("Rebase the WLS domain")
    void rebaseWlsImg(TestInfo testInfo) throws Exception {
        assumeTrue(wlsImgBuilt);
        assumeTrue(domainImgBuilt);
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new RebaseCommand()
            .sourceImage(build_tag, "createWlsImgUsingWdt")
            .targetImage(build_tag, "updateWlsImg")
            .tag(tagName)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }
    }

    /**
     * Create a FMW image with internet access to download PSU.
     * Oracle Support credentials must be provided to download the patches.
     * Uses different JDK version from the default in the Image Tool.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(20)
    @Tag("nightly")
    @DisplayName("Create FMW 12.2.1.3 image with latest PSU")
    void createFmwImgFullInternetAccess(TestInfo testInfo) throws Exception {
        // add jdk 8u212 installer to the cache
        String addNewJdkCmd = new CacheCommand().addInstaller(true)
            .type("jdk")
            .version(JDK_VERSION_8u212)
            .path(STAGING_DIR + FS + JDK_INSTALLER_NEWER)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult addNewJdkResult = Runner.run(addNewJdkCmd, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, addNewJdkResult.exitValue(), "for command: " + addNewJdkCmd);

            // add fmw installer to the cache
            String addCommand = new CacheCommand()
                .addInstaller(true)
                .type("fmw")
                .version(WLS_VERSION)
                .path(STAGING_DIR + FS + FMW_INSTALLER)
                .build();
            CommandResult addResult = Runner.run(addCommand, out, logger);
            // the process return code for addInstaller should be 0
            assertEquals(0, addResult.exitValue(), "for command: " + addCommand);

            String tagName = build_tag + ":" + getMethodName(testInfo);
            // create an an image with FMW and the latest PSU using ARU to download the patch
            String command = new CreateCommand()
                .tag(tagName)
                .jdkVersion(JDK_VERSION_8u212)
                .type("fmw")
                .user(oracleSupportUsername)
                .passwordEnv("ORACLE_SUPPORT_PASSWORD")
                .latestPsu(true)
                .build();

            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }
    }

    /**
     * create a JRF domain image using WDT
     * You need to have OCR credentials to pull container-registry.oracle.com/database/enterprise:12.2.0.1-slim
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(22)
    @Tag("nightly")
    @DisplayName("Create FMW 12.2.1.3 image with WDT domain")
    void testCreateJrfDomainImgUsingWdt(TestInfo testInfo) throws Exception {
        // create a db container for RCU
        createDBContainer();

        // test assumes that WDT installer is already in the cache from previous test

        // test assumes that the FMW 12.2.1.3 installer is already in the cache

        // test assumes that the default JDK version 8u202 is already in the cache

        // build the wdt archive
        buildWdtArchive();

        Path tmpWdtModel = Paths.get(wlsImgBldDir, WDT_MODEL1);

        // update wdt model file
        Path source = Paths.get(WDT_MODEL1);
        Files.copy(source, tmpWdtModel, StandardCopyOption.REPLACE_EXISTING);
        String getDbContainerIp = "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "
            + dbContainerName;

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            String host = Runner.run(getDbContainerIp, out, logger).stdout().trim();
            logger.info("DEBUG: DB_HOST=" + host);
            replaceStringInFile(tmpWdtModel.toString(), "%DB_HOST%", host);

            String tagName = build_tag + ":" + getMethodName(testInfo);
            String command = new CreateCommand()
                .fromImage(BASE_OS_IMG, BASE_OS_IMG_TAG)
                .tag(tagName)
                .version(WLS_VERSION)
                .wdtVersion(WDT_VERSION)
                .wdtArchive(WDT_ARCHIVE)
                .wdtDomainHome("/u01/domains/simple_domain")
                .wdtModel(tmpWdtModel)
                .wdtDomainType("JRF")
                .wdtRunRcu(true)
                .type("fmw")
                .build();

            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }
    }

    /**
     * create a RestrictedJRF domain image using WDT.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(23)
    @Tag("nightly")
    @DisplayName("Create FMW image with WDT domain and latestPSU with new base img")
    void createRestrictedJrfDomainImgUsingWdt(TestInfo testInfo) throws Exception {
        // test assumes that the FMW 12.2.1.3 installer is already in the cache

        // test assumes that the default JDK version 8u202 is already in the cache

        // build the wdt archive
        buildWdtArchive();

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
            .fromImage(BASE_OS_IMG, BASE_OS_IMG_TAG)
            .tag(tagName)
            .version(WLS_VERSION)
            .latestPsu(true)
            .user(oracleSupportUsername)
            .passwordEnv("ORACLE_SUPPORT_PASSWORD")
            .wdtVersion(WDT_VERSION)
            .wdtModel(WDT_MODEL)
            .wdtArchive(WDT_ARCHIVE)
            .wdtVariables(WDT_VARIABLES)
            .wdtDomainHome("/u01/domains/simple_domain")
            .wdtDomainType("RestrictedJRF")
            .type("fmw")
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }
    }

    /**
     * create wls image using multiple WDT model files.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(24)
    @Tag("nightly")
    @DisplayName("Create WLS image with WDT and multiple models")
    void createWlsImgUsingMultiModels(TestInfo testInfo) throws Exception {
        // test assumes that the WLS 12.2.1.3 installer is already in the cache

        // test assumes that the default JDK installer is already in the cache

        // test assumes that the WDT installer is already in the cache

        // build the wdt archive
        buildWdtArchive();

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
            .fromImage(BASE_OS_IMG, BASE_OS_IMG_TAG)
            .tag(tagName)
            .version(WLS_VERSION)
            .wdtVersion(WDT_VERSION)
            .wdtArchive(WDT_ARCHIVE)
            .wdtDomainHome("/u01/domains/simple_domain")
            .wdtModel(WDT_MODEL, WDT_MODEL2)
            .wdtVariables(WDT_VARIABLES)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }
    }

    /**
     * create WLS image with additional build commands.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(25)
    @Tag("nightly")
    @DisplayName("Create image with additionalBuildCommands and recommendedPatches")
    void createWlsImgWithAdditionalBuildCommands(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
            .jdkVersion(JDK_VERSION)
            .tag(tagName)
            .recommendedPatches(true)
            .user(oracleSupportUsername)
            .passwordEnv("ORACLE_SUPPORT_PASSWORD")
            .additionalBuildCommands(WDT_RESOURCES.resolve("multi-sections.txt"))
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            String imageId = Runner.run("docker images -q " + tagName, out, logger).stdout().trim();
            assertFalse(imageId.isEmpty(), "Image was not created: " + tagName);
        }

        // verify the file created in [before-jdk-install] section
        verifyFileInImage(tagName, "/u01/jdk/beforeJDKInstall.txt", "before-jdk-install");
        // verify the file created in [after-jdk-install] section
        verifyFileInImage(tagName, "/u01/jdk/afterJDKInstall.txt", "after-jdk-install");
        // verify the file created in [before-fmw-install] section
        verifyFileInImage(tagName, "/u01/oracle/beforeFMWInstall.txt", "before-fmw-install");
        // verify the file created in [after-fmw-install] section
        verifyFileInImage(tagName, "/u01/oracle/afterFMWInstall.txt", "after-fmw-install");
        // verify the label is created as in [final-build-commands] section
        CommandResult inspect = Runner.run("docker inspect --format '{{ index .Config.Labels}}' " + tagName);
        assertTrue(inspect.stdout().contains("final-build-commands:finalBuildCommands"),
            tagName + " does not contain the expected label");
    }
}
