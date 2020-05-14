// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.oracle.weblogic.imagetool.tests.utils.ExecCommand;
import com.oracle.weblogic.imagetool.tests.utils.ExecResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ITImagetool extends BaseTest {

    private static final String JDK_INSTALLER = "jdk-8u202-linux-x64.tar.gz";
    private static final String JDK_INSTALLER_NEWER = "jdk-8u231-linux-x64.tar.gz";
    private static final String WLS_INSTALLER = "fmw_12.2.1.3.0_wls_Disk1_1of1.zip";
    private static final String P27342434_INSTALLER = "p27342434_122130_Generic.zip";
    private static final String P28186730_INSTALLER = "p28186730_139422_Generic.zip";
    private static final String P22987840_INSTALLER = "p22987840_122100_Generic.zip";
    private static final String WDT_INSTALLER = "weblogic-deploy.zip";
    private static final String FMW_INSTALLER = "fmw_12.2.1.3.0_infrastructure_Disk1_1of1.zip";
    private static final String FMW_INSTALLER_1221 = "fmw_12.2.1.0.0_infrastructure_Disk1_1of1.zip";
    private static final String TEST_ENTRY_KEY = "mytestEntryKey";
    private static final String P27342434_ID = "27342434";
    private static final String P28186730_ID = "28186730";
    private static final String P22987840_ID = "22987840";
    private static final String WLS_VERSION = "12.2.1.3.0";
    private static final String WLS_VERSION_1221 = "12.2.1.0.0";
    private static final String OPATCH_VERSION = "13.9.4.2.2";
    private static final String JDK_VERSION = "8u202";
    private static final String JDK_VERSION_8u212 = "8u212";
    private static final String WDT_VERSION = "1.1.2";
    private static final String WDT_ARCHIVE = "archive.zip";
    private static final String WDT_VARIABLES = "domain.properties";
    private static final String WDT_MODEL = "simple-topology.yaml";
    private static final String WDT_MODEL2 = "simple-topology2.yaml";
    private static String oracleSupportUsername;

    @BeforeAll
    static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");
        // verify that all the prerequisites are set and exist
        validateEnvironmentSettings();
        // clean up Docker instances leftover from a previous run
        cleanup();

        setup();
        // pull base OS docker image used for test
        pullBaseOsDockerImage();
        // pull oracle db image
        pullOracleDbDockerImage();

        // verify that required files/installers are available
        verifyStagedFiles(JDK_INSTALLER, WLS_INSTALLER, WDT_INSTALLER, P27342434_INSTALLER, P28186730_INSTALLER,
            FMW_INSTALLER, JDK_INSTALLER_NEWER, FMW_INSTALLER_1221, P22987840_INSTALLER);

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
     * test cache listItems.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(1)
    void cacheListItems() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        ExecResult result = listItemsInCache();

        // verify the test result
        String expectedString = "cache.dir=" + wlsImgCacheDir;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add JDK installer to the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(2)
    void cacheAddInstallerJDK() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        ExecResult result = listItemsInCache();
        String expectedString = "jdk_" + JDK_VERSION + "=" + jdkPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add WLS installer to the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(3)
    void cacheAddInstallerWls() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String wlsPath = getStagingDir() + FS + WLS_INSTALLER;
        deleteEntryFromCache("wls_" + WLS_VERSION);
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        ExecResult result = listItemsInCache();
        String expectedString = "wls_" + WLS_VERSION + "=" + wlsPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image with default WLS version.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(4)
    void createWlsImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String command = imagetool + " create --jdkVersion=" + JDK_VERSION + " --tag "
            + build_tag + ":" + testMethodName;
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * add Patch to the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(5)
    void cacheAddPatch() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String patchPath = getStagingDir() + FS + P27342434_INSTALLER;
        deleteEntryFromCache(P27342434_ID + "_" + WLS_VERSION);
        addPatchToCache("wls", P27342434_ID, WLS_VERSION, patchPath);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = P27342434_ID + "_" + WLS_VERSION + "=" + patchPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add an entry to the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(6)
    void cacheAddTestEntry() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String mytestEntryValue = getStagingDir() + FS + P27342434_INSTALLER;
        addEntryToCache(TEST_ENTRY_KEY, mytestEntryValue);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = TEST_ENTRY_KEY.toLowerCase() + "=" + mytestEntryValue;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * test delete an entry from the cache.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(7)
    void cacheDeleteTestEntry() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        deleteEntryFromCache(TEST_ENTRY_KEY);

        // verify the result
        ExecResult result = listItemsInCache();
        if (result.exitValue() != 0 || result.stdout().contains(TEST_ENTRY_KEY)) {
            throw new Exception("The entry key is not deleted from the cache");
        }

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image without internet connection.
     * you need to have OCIR credentials to download the base OS docker image
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(8)
    void createWLSImgUseCache() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // need to add the required patches 28186730 for Opatch before create wls images
        String patchPath = getStagingDir() + FS + P28186730_INSTALLER;
        deleteEntryFromCache(P28186730_ID + "_" + OPATCH_VERSION);
        addPatchToCache("wls", P28186730_ID, OPATCH_VERSION, patchPath);
        ExecResult resultT = listItemsInCache();

        String versionTag = "test8img";
        String command = imagetool + " create --jdkVersion " + JDK_VERSION + " --fromImage "
            + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag " + build_tag + ":" + versionTag
            + " --version " + WLS_VERSION;
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(versionTag);

        logTestEnd(testMethodName);
    }

    /**
     * update a WLS image with a patch.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(9)
    void updateWLSImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        ExecResult resultT = listItemsInCache();

        String command = imagetool + " update --fromImage " + build_tag + ":test8img --tag "
            + build_tag + ":" + testMethodName + " --patches " + P27342434_ID;
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image using Weblogic Deploying Tool.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(10)
    void createWlsImgUsingWdt() throws Exception {

        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add WDT installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("wdt_" + WDT_VERSION);
        String wdtPath = getStagingDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add WLS installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("wls_" + WLS_VERSION);
        String wlsPath = getStagingDir() + FS + WLS_INSTALLER;
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        // add jdk installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // need to add the required patches 28186730 for Opatch before create wls images
        // delete the cache entry first
        deleteEntryFromCache(P28186730_ID + "_" + OPATCH_VERSION);
        String patchPath = getStagingDir() + FS + P28186730_INSTALLER;
        addPatchToCache("wls", P28186730_ID, OPATCH_VERSION, patchPath);

        // add the patch to the cache
        deleteEntryFromCache(P27342434_ID + "_" + WLS_VERSION);
        patchPath = getStagingDir() + FS + P27342434_INSTALLER;
        addPatchToCache("wls", P27342434_ID, WLS_VERSION, patchPath);

        // build the wdt archive
        buildWdtArchive();

        String wdtArchive = getWdtResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWdtResourcePath() + FS + WDT_MODEL;
        String wdtVariables = getWdtResourcePath() + FS + WDT_VARIABLES;
        String command = imagetool + " create --fromImage "
            + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag " + build_tag + ":" + testMethodName
            + " --version " + WLS_VERSION + " --patches " + P27342434_ID + " --wdtVersion " + WDT_VERSION
            + " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel "
            + wdtModel + " --wdtVariables " + wdtVariables;

        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a FMW image with full internet access.
     * You need to provide Oracle Support credentials to download the patches
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(11)
    void createFmwImgFullInternetAccess() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add fmw installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("fmw_" + WLS_VERSION);
        String fmwPath = getStagingDir() + FS + FMW_INSTALLER;
        addInstallerToCache("fmw", WLS_VERSION, fmwPath);

        // add jdk installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        String command = imagetool + " create --version=" + WLS_VERSION + " --tag " + build_tag + ":" + testMethodName
            + " --latestPSU --user " + oracleSupportUsername + " --passwordEnv ORACLE_SUPPORT_PASSWORD --type fmw";
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);
        logTestEnd(testMethodName);
    }

    /**
     * create a FMW image with non default JDK, FMW versions.
     * You need to download the jdk, fmw and patch installers from Oracle first
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(12)
    void createFmwImgNonDefault() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add fmw installer to the cache
        String fmwPath = getStagingDir() + FS + FMW_INSTALLER_1221;
        deleteEntryFromCache("fmw_" + WLS_VERSION_1221);
        addInstallerToCache("fmw", WLS_VERSION_1221, fmwPath);

        // add jdk installer to the cache
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER_NEWER;
        deleteEntryFromCache("jdk_" + JDK_VERSION_8u212);
        addInstallerToCache("jdk", JDK_VERSION_8u212, jdkPath);

        // add the patch to the cache
        String patchPath = getStagingDir() + FS + P22987840_INSTALLER;
        deleteEntryFromCache(P22987840_ID + "_" + WLS_VERSION_1221);
        addPatchToCache("fmw", P22987840_ID, WLS_VERSION_1221, patchPath);

        String command = imagetool + " create --jdkVersion " + JDK_VERSION_8u212 + " --version=" + WLS_VERSION_1221
            + " --tag " + build_tag + ":" + testMethodName + " --patches " + P22987840_ID + " --type fmw";
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a JRF domain image using WDT
     * You need to have OCR credentials to pull container-registry.oracle.com/database/enterprise:12.2.0.1-slim
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(13)
    void testDCreateJRFDomainImgUsingWDT() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // create a db container for RCU
        createDBContainer();

        // add WDT installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("wdt_" + WDT_VERSION);
        String wdtPath = getStagingDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add FMW installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("fmw_" + WLS_VERSION);
        String fmwPath = getStagingDir() + FS + FMW_INSTALLER;
        addInstallerToCache("fmw", WLS_VERSION, fmwPath);

        // add jdk installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // build the wdt archive
        buildWdtArchive();

        String wdtArchive = getWdtResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWdtResourcePath() + FS + WDT_MODEL1;
        String tmpWdtModel = wlsImgBldDir + FS + WDT_MODEL1;

        // update wdt model file
        Path source = Paths.get(wdtModel);
        Path dest = Paths.get(tmpWdtModel);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        String getDBContainerIP = "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "
            + dbContainerName;
        String host = ExecCommand.exec(getDBContainerIP).stdout().trim();
        logger.info("DEBUG: DB_HOST=" + host);
        replaceStringInFile(tmpWdtModel, "%DB_HOST%", host);

        String command = imagetool + " create --fromImage "
            + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag " + build_tag + ":" + testMethodName
            + " --version " + WLS_VERSION + " --wdtVersion " + WDT_VERSION
            + " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel "
            + tmpWdtModel + " --wdtDomainType JRF --wdtRunRCU --type fmw";

        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a RestrictedJRF domain image using WDT.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(14)
    void createRestricedJrfDomainImgUsingWdt() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add WDT installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("wdt_" + WDT_VERSION);
        String wdtPath = getStagingDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add FMW installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("fmw_" + WLS_VERSION);
        String fmwPath = getStagingDir() + FS + FMW_INSTALLER;
        addInstallerToCache("fmw", WLS_VERSION, fmwPath);

        // add jdk installer to the cache
        // delete the cache entry if any
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // build the wdt archive
        buildWdtArchive();

        String wdtArchive = getWdtResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWdtResourcePath() + FS + WDT_MODEL;
        String wdtVariables = getWdtResourcePath() + FS + WDT_VARIABLES;
        String command = imagetool + " create --fromImage "
            + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag " + build_tag + ":" + testMethodName
            + " --version " + WLS_VERSION + " --latestPSU --user " + oracleSupportUsername
            + " --passwordEnv ORACLE_SUPPORT_PASSWORD" + " --wdtVersion " + WDT_VERSION
            + " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel "
            + wdtModel + " --wdtDomainType RestrictedJRF --type fmw --wdtVariables " + wdtVariables;

        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create wls image using multiple WDT model files.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(15)
    void createWlsImgUsingMultiModels() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add WDT installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("wdt_" + WDT_VERSION);
        String wdtPath = getStagingDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add WLS installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("wls_" + WLS_VERSION);
        String wlsPath = getStagingDir() + FS + WLS_INSTALLER;
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        // add jdk installer to the cache
        // delete the cache entry first
        deleteEntryFromCache("jdk_" + JDK_VERSION);
        String jdkPath = getStagingDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // need to add the required patches 28186730 for Opatch before create wls images
        // delete the cache entry first
        deleteEntryFromCache(P28186730_ID + "_" + OPATCH_VERSION);
        String patchPath = getStagingDir() + FS + P28186730_INSTALLER;
        addPatchToCache("wls", P28186730_ID, OPATCH_VERSION, patchPath);

        // build the wdt archive
        buildWdtArchive();

        String wdtArchive = getWdtResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWdtResourcePath() + FS + WDT_MODEL;
        String wdtModel2 = getWdtResourcePath() + FS + WDT_MODEL2;
        String wdtVariables = getWdtResourcePath() + FS + WDT_VARIABLES;
        String command = imagetool + " create --fromImage "
            + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag " + build_tag + ":" + testMethodName
            + " --version " + WLS_VERSION + " --wdtVersion " + WDT_VERSION
            + " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel "
            + wdtModel + "," + wdtModel2 + " --wdtVariables " + wdtVariables
            + "--wdtModelOnly";

        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create WLS image with additional build commands.
     *
     * @throws Exception - if any error occurs
     */
    @Test
    @Order(16)
    void createWLSImgWithAdditionalBuildCommands() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String imagename = build_tag + ":" + testMethodName;
        String abcPath = getAbcResourcePath() + FS + "multi-sections.txt";
        String command = imagetool + " create --jdkVersion=" + JDK_VERSION + " --tag "
            + imagename + " --additionalBuildCommands " + abcPath;
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        // verify the file created in [before-jdk-install] section
        verifyFileInImage(imagename, "/u01/jdk/beforeJDKInstall.txt", "before-jdk-install");
        // verify the file created in [after-jdk-install] section
        verifyFileInImage(imagename, "/u01/jdk/afterJDKInstall.txt", "after-jdk-install");
        // verify the file created in [before-fmw-install] section
        verifyFileInImage(imagename, "/u01/oracle/beforeFMWInstall.txt", "before-fmw-install");
        // verify the file created in [after-fmw-install] section
        verifyFileInImage(imagename, "/u01/oracle/afterFMWInstall.txt", "after-fmw-install");
        // verify the label is created as in [final-build-commands] section
        verifyLabelInImage(imagename, "final-build-commands:finalBuildCommands");

        logTestEnd(testMethodName);
    }
}
