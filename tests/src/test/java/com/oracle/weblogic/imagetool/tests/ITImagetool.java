// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.cli.menu.KubernetesTarget;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.tests.annotations.IntegrationTest;
import com.oracle.weblogic.imagetool.tests.annotations.Logger;
import com.oracle.weblogic.imagetool.tests.utils.CacheCommand;
import com.oracle.weblogic.imagetool.tests.utils.CommandResult;
import com.oracle.weblogic.imagetool.tests.utils.CreateAuxCommand;
import com.oracle.weblogic.imagetool.tests.utils.CreateCommand;
import com.oracle.weblogic.imagetool.tests.utils.RebaseCommand;
import com.oracle.weblogic.imagetool.tests.utils.Runner;
import com.oracle.weblogic.imagetool.tests.utils.UpdateCommand;
import com.oracle.weblogic.imagetool.util.Utils;
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
class ITImagetool {

    @Logger
    private static final LoggingFacade logger = LoggingFactory.getLogger(ITImagetool.class);

    // STAGING_DIR - directory where JDK and other installers are pre-staged before testing
    private static final String STAGING_DIR = System.getProperty("STAGING_DIR");
    private static final String wlsImgBldDir = System.getProperty("WLSIMG_BLDDIR");
    private static final String wlsImgCacheDir = System.getProperty("WLSIMG_CACHEDIR");

    // Docker images
    private static String DB_IMAGE = System.getProperty("DB_IMAGE");
    private static String JRE_IMAGE = System.getProperty("JRE_IMAGE");

    // Staging Dir files
    private static final String JDK_INSTALLER = "jdk-8u202-linux-x64.tar.gz";
    private static final String JDK_INSTALLER_NEWER = "jdk-8u231-linux-x64.tar.gz";
    private static final String WLS_INSTALLER = "fmw_12.2.1.3.0_wls_Disk1_1of1.zip";
    private static final String P27342434_INSTALLER = "p27342434_122130_Generic.zip";
    private static final String P28186730_INSTALLER = "p28186730_139422_Generic.zip";
    private static final String WDT_INSTALLER = "weblogic-deploy.zip";
    private static final String FMW_INSTALLER = "fmw_12.2.1.3.0_infrastructure_Disk1_1of1.zip";

    private static final String TEST_ENTRY_KEY = "mytestEntryKey";
    private static final String P27342434_ID = "27342434";
    private static final String P28186730_ID = "28186730";
    private static final String WLS_VERSION = "12.2.1.3.0";
    private static final String OPATCH_VERSION = "13.9.4.2.2";
    private static final String JDK_VERSION = "8u202";
    private static final String JDK_VERSION_8u212 = "8u212";
    private static final String WDT_VERSION = "1.1.2";
    private static final Path WDT_ARCHIVE = Paths.get("target", "wdt", "archive.zip");
    private static final Path WDT_RESOURCES = Paths.get("src", "test", "resources", "wdt");
    private static final Path WDT_VARIABLES = WDT_RESOURCES.resolve("domain.properties");
    private static final Path WDT_MODEL = WDT_RESOURCES.resolve("simple-topology.yaml");
    private static final String WDT_MODEL1 = "simple-topology1.yaml";
    private static final Path WDT_MODEL2 = WDT_RESOURCES.resolve("simple-topology2.yaml");
    private static String dbContainerName = "";
    private static String build_tag = "";
    private static String oracleSupportUsername;
    private static boolean wlsImgBuilt = false;
    private static boolean domainImgBuilt = false;

    private static void validateEnvironmentSettings() {
        logger.info("Initializing the tests ...");

        List<String> missingSettings = new ArrayList<>();
        if (Utils.isEmptyString(wlsImgBldDir)) {
            missingSettings.add("WLSIMG_BLDDIR");
        }

        if (Utils.isEmptyString(wlsImgCacheDir)) {
            missingSettings.add("WLSIMG_CACHEDIR");
        }

        if (Utils.isEmptyString(STAGING_DIR)) {
            missingSettings.add("STAGING_DIR");
        }

        if (missingSettings.size() > 0) {
            String error = String.join(", ", missingSettings)
                + " must be set as a system property in the pom.xml";
            throw new IllegalArgumentException(error);
        }

        if (Utils.isEmptyString(DB_IMAGE)) {
            //default to container-registry.oracle.com image
            DB_IMAGE = "container-registry.oracle.com/database/enterprise:12.2.0.1-slim";
        }

        if (Utils.isEmptyString(JRE_IMAGE)) {
            //default to container-registry.oracle.com image
            JRE_IMAGE = "container-registry.oracle.com/java/serverjre:8";
        }

        // get the build tag from Jenkins build environment variable BUILD_TAG
        build_tag = System.getenv("BUILD_TAG");
        if (build_tag != null) {
            build_tag = build_tag.toLowerCase().replaceAll("\\s+", "-");
        } else {
            build_tag = "imagetool-itest";
        }
        dbContainerName = "InfraDB4" + build_tag;
        logger.info("build_tag = " + build_tag);
        logger.info("WLSIMG_BLDDIR = " + wlsImgBldDir);
        logger.info("WLSIMG_CACHEDIR = " + wlsImgCacheDir);
        logger.info("STAGING_DIR = " + STAGING_DIR);
        logger.info("DB_IMAGE = " + DB_IMAGE);
        logger.info("JRE_IMAGE = " + JRE_IMAGE);
    }

    private static void verifyStagedFiles(String... installers) {
        // determine if any of the required installers are missing from the stage directory
        List<String> missingInstallers = new ArrayList<>();
        for (String installer : installers) {
            Path installFile = Paths.get(STAGING_DIR, installer);
            if (!Files.exists(installFile)) {
                missingInstallers.add(installer);
            }
        }
        if (missingInstallers.size() > 0) {
            String error = "Could not find these installers in the staging directory: " + STAGING_DIR + "\n   ";
            error += String.join("\n   ", missingInstallers);
            throw new IllegalStateException(error);
        }
    }

    private static void executeNoVerify(String command) throws Exception {
        logger.info("executing command: " + command);
        Runner.run(command);
    }

    private static void checkCmdInLoop(String cmd) throws Exception {
        final int maxIterations = 50;
        final String matchStr = "healthy";

        int i = 0;
        while (i < maxIterations) {
            CommandResult result = Runner.run(cmd);

            // pod might not have been created or if created loop till condition
            if (result.exitValue() != 0
                || (result.exitValue() == 0 && !result.stdout().contains(matchStr))) {
                // check for last iteration
                if (i == (maxIterations - 1)) {
                    throw new RuntimeException(
                        "FAILURE: " + cmd + " does not return the expected string " + matchStr + ", exiting!");
                }
                final int waitTime = 5;
                logger.info("Waiting for the expected String {0}: Iter [{1}/{2}], sleeping {3} seconds more",
                    matchStr, i, maxIterations, waitTime);

                Thread.sleep(waitTime * 1000);
                i++;
            } else {
                logger.info("get the expected String " + matchStr);
                break;
            }
        }
    }

    private static void cleanup() throws Exception {
        logger.info("cleaning up the test environment ...");

        // clean up the db container
        String command = "docker rm -f -v " + dbContainerName;
        executeNoVerify(command);

        // clean up the images created in the tests
        command = "docker rmi -f $(docker images -q '" + build_tag + "' | uniq)";
        executeNoVerify(command);
    }

    @BeforeAll
    static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");
        // verify that all the prerequisites are set and exist
        validateEnvironmentSettings();
        // clean up Docker instances leftover from a previous run
        cleanup();

        logger.info("Setting up the test environment ...");

        if (!(new File(wlsImgBldDir)).exists()) {
            logger.info(wlsImgBldDir + " does not exist, creating it");
            if (!(new File(wlsImgBldDir)).mkdir()) {
                throw new IllegalStateException("Unable to create build directory " + wlsImgBldDir);
            }
        }

        // verify that required files/installers are available
        verifyStagedFiles(JDK_INSTALLER, WLS_INSTALLER, WDT_INSTALLER, P27342434_INSTALLER, P28186730_INSTALLER,
            FMW_INSTALLER, JDK_INSTALLER_NEWER);

        // get Oracle support credentials
        oracleSupportUsername = System.getenv("ORACLE_SUPPORT_USERNAME");
        String oracleSupportPassword = System.getenv("ORACLE_SUPPORT_PASSWORD");
        if (oracleSupportUsername == null || oracleSupportPassword == null) {
            throw new Exception("Please set environment variables ORACLE_SUPPORT_USERNAME and ORACLE_SUPPORT_PASSWORD"
                + " for Oracle Support credentials to download the patches.");
        }

        logger.info("Building WDT archive ...");
        Path scriptPath = Paths.get("src", "test", "resources", "wdt", "build-archive.sh");
        String command = "sh " + scriptPath;
        CommandResult result = executeAndVerify(command);
        if (result.exitValue() != 0) {
            logger.severe(result.stdout());
            throw new IOException("Failed to build WDT Archive");
        }
    }

    @AfterAll
    static void staticUnprepare() throws Exception {
        logger.info("cleaning up after the test ...");
        cleanup();
    }

    /**
     * Create the log directory in ./target (build folder), and open a new file using the test method's name.
     *
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
                new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(outputPath))), true);
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

    private static CommandResult executeAndVerify(String command) throws Exception {
        logger.info("Executing command: " + command);
        CommandResult result = Runner.run(command);
        assertEquals(0, result.exitValue(), "for command: " + command);
        logger.info(result.stdout());
        return result;
    }

    /**
     * Determine if a Docker image exists on the local system.
    */
    private static boolean imageExists(String imageTag) throws IOException, InterruptedException {
        return !getImageId(imageTag).isEmpty();
    }

    /**
     * Get the docker identifier for this image tag.
     */
    private static String getImageId(String imageTag) throws IOException, InterruptedException {
        return Runner.run("docker images -q " + imageTag).stdout().trim();
    }

    private void verifyFileInImage(String imagename, String filename, String expectedContent) throws Exception {
        logger.info("verifying the file content in image");
        String command = "docker run --rm " + imagename + " sh -c 'cat " + filename + "'";
        logger.info("executing command: " + command);
        CommandResult result = Runner.run(command);
        if (!result.stdout().contains(expectedContent)) {
            throw new Exception("The image " + imagename + " does not have the expected file content: "
                + expectedContent);
        }
    }

    private void createDBContainer() throws Exception {
        logger.info("Creating an Oracle db docker container ...");
        String command = "docker rm -f " + dbContainerName;
        Runner.run(command);
        command = "docker run -d --name " + dbContainerName + " --env=\"DB_PDB=InfraPDB1\""
            + " --env=\"DB_DOMAIN=us.oracle.com\" --env=\"DB_BUNDLE=basic\" " + DB_IMAGE;
        logger.info("executing command: " + command);
        Runner.run(command);

        // wait for the db is ready
        command = "docker ps | grep " + dbContainerName;
        checkCmdInLoop(command);
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
        Path jdkPath = Paths.get(STAGING_DIR, JDK_INSTALLER);
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
        Path wlsPath = Paths.get(STAGING_DIR, WLS_INSTALLER);
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
        Path patchPath = Paths.get(STAGING_DIR, P27342434_INSTALLER);
        String command = new CacheCommand()
            .addPatch(true)
            .path(patchPath)
            .patchId(P27342434_ID, WLS_VERSION)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addPatch should be 0
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
        Path testEntryValue = Paths.get(STAGING_DIR, P27342434_INSTALLER);
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
        Path patchPath = Paths.get(STAGING_DIR, P28186730_INSTALLER);
        String command = new CacheCommand()
            .addPatch(true)
            .path(patchPath)
            .patchId(P28186730_ID, OPATCH_VERSION)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            // the process return code for addPatch should be 0
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
     * Add WDT installer to the cache.
     * @throws IOException if getting a file to write the command output fails
     * @throws InterruptedException if running the Java command fails
     */
    @Test
    @Order(7)
    @Tag("gate")
    @Tag("cache")
    @DisplayName("Add WDT installer to cache")
    void cacheAddInstallerWdt(TestInfo testInfo) throws IOException, InterruptedException {
        // add WDT installer to the cache
        Path wdtPath = Paths.get(STAGING_DIR, WDT_INSTALLER);
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

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

        try (PrintWriter out = getTestMethodWriter(testInfo)) {

            String tagName = build_tag + ":" + getMethodName(testInfo);
            // create a WLS image with a domain
            String command = new CreateCommand()
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
        }
    }

    /**
     * Create an image with WDT Model on OL 8-slim
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(14)
    @Tag("gate")
    @DisplayName("Create Model in Image with OL 8-slim")
    void createMiiOl8slim(TestInfo testInfo) throws Exception {
        // test assumes that WDT installer is already in the cache from previous test

        // test assumes that the WLS 12.2.1.3 installer is already in the cache

        // test assumes that the default JDK version 8u202 is already in the cache

        Path tmpWdtModel = Paths.get(wlsImgBldDir, WDT_MODEL1);

        // update wdt model file
        Files.copy(WDT_RESOURCES.resolve(WDT_MODEL1), tmpWdtModel, StandardCopyOption.REPLACE_EXISTING);

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            String tagName = build_tag + ":" + getMethodName(testInfo);
            String command = new CreateCommand()
                .tag(tagName)
                .fromImage("ghcr.io/oracle/oraclelinux", "8-slim")
                .version(WLS_VERSION)
                .wdtVersion(WDT_VERSION)
                .wdtArchive(WDT_ARCHIVE)
                .wdtModel(tmpWdtModel)
                .wdtModelOnly(true)
                .type("wls")
                .build();

            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

            verifyFilePermissions("/u01/domains", "drwxr-xr-x", tagName, out);
            verifyFilePermissions("/u01/wdt", "drwxr-xr-x", tagName, out);
            verifyFilePermissions("/u01/wdt/models", "drwxr-xr-x", tagName, out);
            verifyFilePermissions("/u01/wdt/weblogic-deploy", "drwxr-x---", tagName, out);
            verifyFilePermissions("/u01/oracle", "drwxr-xr-x", tagName, out);
            verifyFilePermissions("/u01/wdt/weblogic-deploy/bin/createDomain.sh", "-rwxr-x---", tagName, out);
            verifyFilePermissions("/u01/wdt/weblogic-deploy/bin/validateModel.sh", "-rwxr-x---", tagName, out);
        }
    }

    /**
     * Use the Rebase function to move a domain to a new image.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(15)
    @Tag("gate")
    @DisplayName("Create Aux Image")
    void createAuxImage(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateAuxCommand()
            .tag(tagName)
            .wdtModel(WDT_MODEL)
            .wdtArchive(WDT_ARCHIVE)
            .wdtVersion(WDT_VERSION)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

            // verify the file created in [before-jdk-install] section
            verifyFileInImage(tagName, "/auxiliary/models/simple-topology.yaml", "AdminUserName: weblogic");
            verifyFilePermissions("/auxiliary/models/archive.zip", "-rw-r-----", tagName, out);
            verifyFilePermissions("/auxiliary/models/archive.zip", "-rw-r-----", tagName, out);
            verifyFilePermissions("/auxiliary/weblogic-deploy/bin/createDomain.sh", "-rwxr-x---", tagName, out);
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
            .path(Paths.get(STAGING_DIR, JDK_INSTALLER_NEWER))
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
                .path(Paths.get(STAGING_DIR, FMW_INSTALLER))
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
        }
    }

    /**
     * create a JRF domain image using WDT
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(22)
    @Tag("nightly")
    @DisplayName("Create FMW 12.2.1.3 image with WDT domain")
    void createJrfDomainImgUsingWdt(TestInfo testInfo) throws Exception {
        // create a db container for RCU
        createDBContainer();

        // test assumes that WDT installer is already in the cache from previous test

        // test assumes that the FMW 12.2.1.3 installer is already in the cache

        // test assumes that the default JDK version 8u202 is already in the cache

        Path tmpWdtModel = Paths.get(wlsImgBldDir, WDT_MODEL1);

        // update wdt model file
        Files.copy(WDT_RESOURCES.resolve(WDT_MODEL1), tmpWdtModel, StandardCopyOption.REPLACE_EXISTING);
        String getDbContainerIp = "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "
            + dbContainerName;

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            String host = Runner.run(getDbContainerIp, out, logger).stdout().trim();
            logger.info("Setting WDT Model DB_HOST to {0}", host);
            String content = new String(Files.readAllBytes(tmpWdtModel));
            content = content.replaceAll("%DB_HOST%", host);
            Files.write(tmpWdtModel, content.getBytes());

            String tagName = build_tag + ":" + getMethodName(testInfo);
            String command = new CreateCommand()
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
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

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
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

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
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
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
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

    /**
     * Create a WLS image using Java Server JRE image as a base.
     * This tests that the JAVA_HOME is correctly identified and applied in the CREATE.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(26)
    @Tag("nightly")
    @DisplayName("Create image with WLS using Java ServerJRE")
    void createImageWithServerJRE(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
            .tag(tagName)
            .fromImage(JRE_IMAGE)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
        }
    }

    /**
     * Update the WLS image created in the previous test.
     * This tests that the JAVA_HOME is correctly identified and applied in the UPDATE.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(27)
    @Tag("nightly")
    @DisplayName("Update image with WLS using Java ServerJRE")
    void updateImageWithServerJRE(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new UpdateCommand()
            .fromImage(build_tag + ":createImageWithServerJRE")
            .tag(tagName)
            .patches(P27342434_ID)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
        }
    }

    /**
     * Verify file permissions for a specified path on the given image.
     * @param path      Filename or Directory to check for permissions value.
     * @param expected  Expected permission string, such as "drwxrwxr-x"
     * @param tagName   Tag name or image ID of the image to inspect
     * @param out       The printwriter where the docker run command will send stdout/stderr
     * @throws IOException if process start fails
     * @throws InterruptedException if the wait is interrupted before the process completes
     */
    private void verifyFilePermissions(String path, String expected, String tagName, PrintWriter out)
        throws IOException, InterruptedException {
        String command = String.format(" docker run --rm -t %s ls -ld %s", tagName, path);
        String actual = Runner.run(command, out, logger).stdout().trim();
        String[] tokens = actual.split(" ", 2);
        assertEquals(2, tokens.length, "Unable to get file permissions for " + path);
        // When running on an SELinux host, the permissions shown by ls will end with a "."
        assertEquals(expected, tokens[0].substring(0,expected.length()), "Incorrect file permissions for " + path);
    }

    /**
     * update a WLS image with a model.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(28)
    @Tag("nightly")
    @DisplayName("Use Update to add a WDT model to createWlsImg")
    void updateAddModel(TestInfo testInfo) throws Exception {
        assumeTrue(wlsImgBuilt);

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new UpdateCommand()
            .fromImage(build_tag + ":createWlsImg") //from step 10, createWlsImg()
            .tag(tagName)
            .wdtVersion(WDT_VERSION)
            .wdtModel(WDT_MODEL)
            .wdtVariables(WDT_VARIABLES)
            .wdtArchive(WDT_ARCHIVE)
            .wdtModelOnly(true)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
            verifyFilePermissions("/u01/wdt/weblogic-deploy/bin/createDomain.sh", "-rwxr-x---", tagName, out);
            verifyFilePermissions("/u01/wdt/weblogic-deploy/bin/validateModel.sh", "-rwxr-x---", tagName, out);
        }
    }

    /**
     * update a WLS image with another model.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(29)
    @Tag("nightly")
    @DisplayName("Use Update to add a second WDT model to createWlsImg")
    void updateAddSecondModel(TestInfo testInfo) throws Exception {
        String testFromImage = build_tag + ":updateAddModel";
        // skip this test if updateAddModel() failed to create an image
        assumeTrue(imageExists(testFromImage));

        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new UpdateCommand()
            .fromImage(testFromImage)
            .tag(tagName)
            .wdtVersion(WDT_VERSION)
            .wdtModel(WDT_MODEL2)
            .wdtModelOnly(true)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);
        }
    }

    /**
     * create WLS image with OpenShift settings.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(30)
    @Tag("nightly")
    @DisplayName("Create image with OpenShift settings")
    void createWlsImgWithOpenShiftSettings(TestInfo testInfo) throws Exception {
        String tagName = build_tag + ":" + getMethodName(testInfo);
        String command = new CreateCommand()
            .jdkVersion(JDK_VERSION)
            .tag(tagName)
            .wdtVersion(WDT_VERSION)
            .wdtArchive(WDT_ARCHIVE)
            .wdtDomainHome("/u01/domains/simple_domain")
            .wdtModel(WDT_MODEL, WDT_MODEL2)
            .wdtVariables(WDT_VARIABLES)
            .target(KubernetesTarget.OPENSHIFT)
            .build();

        try (PrintWriter out = getTestMethodWriter(testInfo)) {
            CommandResult result = Runner.run(command, out, logger);
            assertEquals(0, result.exitValue(), "for command: " + command);

            // verify the docker image is created
            assertTrue(imageExists(tagName), "Image was not created: " + tagName);

            // verify the file permissions on the domain directory were set correctly
            verifyFilePermissions("/u01/domains/simple_domain", "drwxrwxr-x", tagName, out);
        }

    }
}
