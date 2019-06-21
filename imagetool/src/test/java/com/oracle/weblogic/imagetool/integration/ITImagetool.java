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

    private static final String JDK_INSTALLER = "jdk-8u212-linux-x64.tar.gz";
    private static final String WLS_INSTALLER = "fmw_12.2.1.3.0_wls_Disk1_1of1.zip";
    private static final String P27342434_INSTALLER = "p27342434_122130_Generic.zip";
    private static final String P28186730_INSTALLER = "p28186730_139400_Generic.zip";
    private static final String TEST_ENTRY_KEY = "mytestEntryKey";
    private static final String P27342434_ID = "27342434";
    private static final String P28186730_ID = "28186730";
    private static final String WLS_VERSION = "12.2.1.3.0";
    private static final String JDK_VERSION = "8u212";

    @BeforeClass
    public static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");
        initialize();
        setup();
        // pull base OS docker image used for test
        pullDockerImage();
    }

    @AfterClass
    public static void staticUnprepare() throws Exception {
        logger.info("cleaning up after the test ...");
    }

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

    @Test
    public void test2CacheAddInstallerJDK() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String jdkPath = getProjectRoot() + FS + ".." + FS + "caches" + FS + JDK_INSTALLER;
        addInstallerToCache("jdk", JDK_VERSION, jdkPath);

        ExecResult result = listItemsInCache();
        String expectedString = "jdk_" + JDK_VERSION + "=" + jdkPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    @Test
    public void test3CacheAddInstallerWLS() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String wlsPath =  getProjectRoot() + FS + ".." + FS + "caches" + FS + WLS_INSTALLER;
        addInstallerToCache("wls", WLS_VERSION, wlsPath);

        ExecResult result = listItemsInCache();
        String expectedString = "wls_" + WLS_VERSION + "=" + wlsPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    @Test
    public void test4CreateWLSImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String command = imagetool + " create --jdkVersion=" + JDK_VERSION + " --tag imagetool:" + testMethodName;
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        ExecResult result = ExecCommand.exec("docker images | grep imagetool | grep " + testMethodName +
                "| wc -l");
        if(Integer.parseInt(result.stdout()) != 1) {
            throw new Exception("wls docker image is not created as expected");
        }

        logTestEnd(testMethodName);
    }

    @Test
    public void test5CacheAddPatch() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String patchPath = getProjectRoot() + FS + ".." + FS + "caches" + FS + P27342434_INSTALLER;
        addPatchToCache("wls", "p" + P27342434_ID, WLS_VERSION, patchPath);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = P27342434_ID + "_" + WLS_VERSION + "=" + patchPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

    @Test
    public void test6CacheAddEntry() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String mytestEntryValue = getProjectRoot() + FS + ".." + FS + "caches" + FS + P27342434_INSTALLER;
        addEntryToCache(TEST_ENTRY_KEY, mytestEntryValue);

        // verify the result
        ExecResult result = listItemsInCache();
        String expectedString = TEST_ENTRY_KEY.toLowerCase() + "=" + mytestEntryValue;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }

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

    @Test
    public void test8CreateWLSImgUseCache() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        // need to add the required patches 28186730 for Opatch before create wls images
        String patchPath = getProjectRoot() + FS + ".." + FS + "caches" + FS + P28186730_INSTALLER;
        addPatchToCache("wls", "p" + P28186730_ID, WLS_VERSION, patchPath);

        String command = imagetool + " create --jdkVersion " + JDK_VERSION + " --fromImage " +
                BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " --tag imagetool:" + testMethodName +
                " --version " + WLS_VERSION + " --useCache always";
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        ExecResult result = ExecCommand.exec("docker images | grep imagetool | grep " + testMethodName +
                "| wc -l");
        if(Integer.parseInt(result.stdout()) != 1) {
            throw new Exception("wls docker image is not created as expected");
        }

        logTestEnd(testMethodName);
    }

    @Test
    public void test9UpdateWLSImg() throws Exception {
        String testMethodName = new Object() {}.getClass().getEnclosingMethod().getName();
        logTestBegin(testMethodName);

        String command = imagetool + " update --fromImage imagetool:test8CreateWLSImgUseCache --tag imagetool:" +
                testMethodName + " --patches " + P27342434_ID + " --useCache always";
        logger.info("Executing command: " + command);
        ExecCommand.exec(command, true);

        // verify the docker image is created
        ExecResult result = ExecCommand.exec("docker images | grep imagetool | grep " + testMethodName +
                "| wc -l");
        if(Integer.parseInt(result.stdout()) != 1) {
            throw new Exception("wls docker image is not created as expected");
        }

        logTestEnd(testMethodName);
    }
}
