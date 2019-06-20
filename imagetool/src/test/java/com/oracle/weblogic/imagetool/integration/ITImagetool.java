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



    @BeforeClass
    public static void staticPrepare() throws Exception {
        logger.info("prepare for image tool test ...");
        initialize();
        setup();
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

        String jdkPath = getProjectRoot() + FS + ".." + FS + "caches" + FS + "jdk-8u212-linux-x64.tar.gz";
        ExecResult result = addInstallerToCache("jdk", "8u212", jdkPath);
        logger.info(result.stderr());

        result = listItemsInCache();
        String expectedString = "jdk_8u212=" + jdkPath;
        verifyResult(result, expectedString);

        logTestEnd(testMethodName);
    }
}
