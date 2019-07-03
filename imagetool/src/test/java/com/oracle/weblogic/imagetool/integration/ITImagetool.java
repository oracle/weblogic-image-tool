// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.integration;

import com.oracle.weblogic.imagetool.integration.utils.ExecCommand;
import com.oracle.weblogic.imagetool.integration.utils.ExecResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ITImagetool extends BaseTest {

    private static final String JDK_INSTALLER = "jdk-8u202-linux-x64.tar.gz";
    private static final String JDK_INSTALLER_8u212 = "jdk-8u212-linux-x64.tar.gz";
    private static final String WLS_INSTALLER = "fmw_12.2.1.3.0_wls_Disk1_1of1.zip";
    private static final String P27342434_INSTALLER = "p27342434_122130_Generic.zip";
    private static final String P28186730_INSTALLER = "p28186730_139400_Generic.zip";
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
    private static final String OPATCH_VERSION = "13.9.4.0.0";
    private static final String JDK_VERSION = "8u202";
    private static final String JDK_VERSION_8u212 = "8u212";
    private static final String WDT_VERSION = "1.1.2";
    private static final String WDT_ARCHIVE = "archive.zip";
    private static final String WDT_VARIABLES = "domain.properties";
    private static final String WDT_MODEL = "simple-topology.yaml";
    private static final String WDT_MODEL1 = "simple-topology1.yaml";

    @BeforeClass
    public static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");

        initialize();
        // clean up the env first
        cleanup();

        setup();
        // pull base OS docker image used for test
        pullBaseOSDockerImage();

        // download the installers for the test
        downloadInstallers(JDK_INSTALLER, WLS_INSTALLER, WDT_INSTALLER, P27342434_INSTALLER, P28186730_INSTALLER,
                FMW_INSTALLER);
    }

    @AfterClass
    public static void staticUnprepare() throws Exception {
        logger.info("cleaning up after the test ...");
        cleanup();
    }

    /**
     * test cache listItems
     * @throws Exception - if any error occurs
     */
    @Test
    public void test1CacheListItems() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        ExecResult result = listItemsInCache();

        // verify the test result
        String expectedString = "cache.dir=" + wlsImgCacheDir;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add JDK installer to the cache
     * @throws Exception - if any error occurs
     */
    @Test
    public void test2CacheAddInstallerJDK() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String jdkPath = getInstallerCacheDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        ExecResult result = listItemsInCache();
        String expectedString = "jdk_" + JDK_VERSION + "=" + jdkPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add WLS installer to the cache
     * @throws Exception - if any error occurs
     */
    @Test
    public void test3CacheAddInstallerWLS() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String wlsPath =  getInstallerCacheDir() + FS + WLS_INSTALLER;
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        ExecResult result = listItemsInCache();
        String expectedString = "wls_" + WLS_VERSION + "=" + wlsPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image with default WLS version
     * @throws Exception
     */
    @Test
    public void test4CreateWLSImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String command = imagetool + " create --jdkVersion=" + JDK_VERSION + " --tag imagetool:" + testMethodName;
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * add Patch to the cache
     * @throws Exception - if any error occurs
     */
    @Test
    public void test5CacheAddPatch() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String patchPath = getInstallerCacheDir() + FS + P27342434_INSTALLER;
        deleteEntryFromCache(P27342434_ID + "_" + WLS_VERSION);
        addPatchToCache("wls", P27342434_ID, WLS_VERSION, patchPath);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = P27342434_ID + "_" + WLS_VERSION + "=" + patchPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * add an entry to the cache
     * @throws Exception - if any error occurs
     */
    @Test
    public void test6CacheAddEntry() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String mytestEntryValue = getInstallerCacheDir() + FS + P27342434_INSTALLER;
        addEntryToCache(TEST_ENTRY_KEY, mytestEntryValue);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = TEST_ENTRY_KEY.toLowerCase() + "=" + mytestEntryValue;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    /**
     * test delete an entry from the cache
     * @throws Exception - if any error occurs
     */
    @Test
    public void test7CacheDeleteEntry() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        deleteEntryFromCache(TEST_ENTRY_KEY);

        // verify the result
        ExecResult result = listItemsInCache();
        if(result.exitValue() != 0 || result.stdout().contains(TEST_ENTRY_KEY)) {
            throw new Exception("The entry key is not deleted from the cache");
        }

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image without internet connection
     * you need to have OCIR credentials to download the base OS docker image
     * @throws Exception - if any error occurs
     */
    @Test
    public void test8CreateWLSImgUseCache() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // need to add the required patches 28186730 for Opatch before create wls images
        String patchPath = getInstallerCacheDir() + FS + P28186730_INSTALLER;
        addPatchToCache("wls", P28186730_ID, OPATCH_VERSION, patchPath);

        String command = imagetool + " create --jdkVersion " + JDK_VERSION + " --fromImage " +
                BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag imagetool:" + testMethodName +
                " --version " + WLS_VERSION;
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * update a WLS image with a patch
     * @throws Exception - if any error occurs
     */
    @Test
    public void test9UpdateWLSImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String command = imagetool + " update --fromImage imagetool:test8CreateWLSImgUseCache --tag imagetool:" +
                testMethodName + " --patches " + P27342434_ID;
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a WLS image using Weblogic Deploying Tool
     * @throws Exception - if any error occurs
     */
    @Test
    public void testACreateWLSImgUsingWDT() throws Exception {

        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // add WDT installer to the cache
        String wdtPath = getInstallerCacheDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add WLS installer to the cache
        String wlsPath =  getInstallerCacheDir() + FS + WLS_INSTALLER;
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        // add jdk installer to the cache
        String jdkPath = getInstallerCacheDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // need to add the required patches 28186730 for Opatch before create wls images
        // delete the cache entry first
        deleteEntryFromCache(P28186730_ID + "_opatch");
        String patchPath = getInstallerCacheDir() + FS + P28186730_INSTALLER;
        addPatchToCache("wls", P28186730_ID, OPATCH_VERSION, patchPath);

        // add the patch to the cache
        deleteEntryFromCache(P27342434_ID + "_" + WLS_VERSION);
        patchPath = getInstallerCacheDir() + FS + P27342434_INSTALLER;
        addPatchToCache("wls", P27342434_ID, WLS_VERSION, patchPath);

        // build the wdt archive
        buildWDTArchive();

        String wdtArchive = getWDTResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWDTResourcePath() + FS + WDT_MODEL;
        String wdtVariables = getWDTResourcePath() + FS + WDT_VARIABLES;
        String command = imagetool + " create --fromImage " +
                BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag imagetool:" + testMethodName +
                " --version " + WLS_VERSION + " --patches " + P27342434_ID + " --wdtVersion " + WDT_VERSION +
                " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel " +
                wdtModel + " --wdtVariables " + wdtVariables;

        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a FMW image with full internet access
     * You need to provide Oracle Support credentials to download the patches
     * @throws Exception - if any error occurs
     */
    @Test
    public void testBCreateFMWImgFullInternetAccess() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String user = System.getenv("ORACLE_SUPPORT_USERNAME");
        String password = System.getenv("ORACLE_SUPPORT_PASSWORD");
        if(user == null || password == null) {
            throw new Exception("Please set environment variables ORACLE_SUPPORT_USERNAME and ORACLE_SUPPORT_PASSWORD" +
            " for Oracle Support credentials to download the patches.");
        }

        String httpProxy = System.getenv("HTTP_PROXY");
        String httpsProxy = System.getenv("HTTPS_PROXY");
        if(httpProxy == null || httpsProxy == null) {
            throw new Exception("Please set environment variable HTTP_PROXY and HTTPS_PROXY");
        }

        // add fmw installer to the cache
        String fmwPath =  getInstallerCacheDir() + FS + FMW_INSTALLER;
        addInstallerToCache("fmw", WLS_VERSION, fmwPath);

        // add jdk installer to the cache
        String jdkPath = getInstallerCacheDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        String command = imagetool + " create --version=" + WLS_VERSION + " --tag imagetool:" + testMethodName +
                " --latestPSU --user " + user + " --passwordEnv ORACLE_SUPPORT_PASSWORD --httpProxyUrl " +
                httpProxy + " --httpsProxyUrl " + httpsProxy + " --type fmw";
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);
        logTestEnd(testMethodName);
    }

    /**
     * create a FMW image with non default JDK, FMW versions
     * You need to download the jdk, fmw and patch installers from Oracle first
     * @throws Exception - if any error occurs
     */
    @Test
    public void testCCreateFMWImgNonDefault() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // download the installers with non default version
        downloadInstallers(JDK_INSTALLER_8u212, FMW_INSTALLER_1221, P22987840_INSTALLER);

        // add fmw installer to the cache
        String fmwPath =  getInstallerCacheDir() + FS + FMW_INSTALLER_1221;
        addInstallerToCache("fmw", WLS_VERSION_1221, fmwPath);

        // add jdk installer to the cache
        String jdkPath = getInstallerCacheDir() + FS + JDK_INSTALLER_8u212;
        addInstallerToCache("jdk", JDK_VERSION_8u212, jdkPath);

        // add the patch to the cache
        String patchPath = getInstallerCacheDir() + FS + P22987840_INSTALLER;
        addPatchToCache("fmw", P22987840_ID, WLS_VERSION_1221, patchPath);

        String command = imagetool + " create --jdkVersion " + JDK_VERSION_8u212 + " --version=" + WLS_VERSION_1221 +
                " --tag imagetool:" + testMethodName + " --patches " + P22987840_ID + " --type fmw";
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }

    /**
     * create a JRF domain image using WDT
     * You need to have OCR credentials to pull container-registry.oracle.com/database/enterprise:12.2.0.1-slim
     * @throws Exception
     */
    @Test
    public void testDCreateJRFDomainImgUsingWDT() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // pull oracle db image
        pullOracleDBDockerImage();

        // create a db container for RCU
        createDBContainer();

        // add WDT installer to the cache
        String wdtPath = getInstallerCacheDir() + FS + WDT_INSTALLER;
        addInstallerToCache("wdt", WDT_VERSION, wdtPath);

        // add FMW installer to the cache
        String fmwPath =  getInstallerCacheDir() + FS + FMW_INSTALLER;
        addInstallerToCache("fmw", WLS_VERSION, fmwPath);

        // add jdk installer to the cache
        String jdkPath = getInstallerCacheDir() + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        // build the wdt archive
        buildWDTArchive();

        String wdtArchive = getWDTResourcePath() + FS + WDT_ARCHIVE;
        String wdtModel = getWDTResourcePath() + FS + WDT_MODEL1;

        // update wdt model file
        String host = System.getenv("HOST");
        if (host == null) {
            throw new Exception("There is no HOST environment variable defined");
        }
        replaceStringInFile(wdtModel, "%DB_HOST%", host);

        String command = imagetool + " create --fromImage " +
                BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag imagetool:" + testMethodName +
                " --version " + WLS_VERSION + " --wdtVersion " + WDT_VERSION +
                " --wdtArchive " + wdtArchive + " --wdtDomainHome /u01/domains/simple_domain --wdtModel " +
                wdtModel + " --wdtDomainType JRF --wdtRunRCU --type fmw";

        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        verifyDockerImages(testMethodName);

        logTestEnd(testMethodName);
    }
}
