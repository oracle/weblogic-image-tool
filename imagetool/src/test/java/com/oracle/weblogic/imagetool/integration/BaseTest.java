// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.integration;

import com.oracle.weblogic.imagetool.integration.utils.ExecCommand;
import com.oracle.weblogic.imagetool.integration.utils.ExecResult;

import java.io.File;
import java.util.logging.Logger;

public class BaseTest {


    protected static final Logger logger = Logger.getLogger(ITImagetool.class.getName());
    protected static final String VERSION = "1.0.1";
    protected static final String PS = File.pathSeparator;
    protected static final String FS = File.separator;
    private static final String OCIR_SERVER = "phx.ocir.io";
    private static final String OCIR_TENENT = "weblogick8s";
    protected static final String BASE_OS_IMG = "phx.ocir.io/weblogick8s/oraclelinux";
    protected static final String BASE_OS_IMG_TAG = "7-4imagetooltest";

    private static String projectRoot = "";
    protected static String wlsImgBldDir = "";
    protected static String wlsImgCacheDir = "";
    protected static String imagetool = "";
    private static String imagetoolZipfile = "";


    protected static void initialize() throws Exception {
        logger.info("Initializing the tests ...");

        projectRoot = System.getProperty("user.dir");

        if(System.getenv("WLSIMG_BLDDIR") != null) {
            wlsImgBldDir = System.getenv("WLSIMG_BLDDIR");
        } else {
            wlsImgBldDir = System.getenv("HOME");
        }
        if(System.getenv("WLSIMG_CACHEDIR") != null) {
            wlsImgCacheDir = System.getenv("WLSIMG_CACHEDIR");
        } else {
            wlsImgCacheDir = System.getenv("HOME") + FS + "cache";
        }

        imagetoolZipfile = "imagetool-" + VERSION + "-SNAPSHOT.zip";

        // imagetool = "java -cp " + getImagetoolHome() + FS + "lib" + FS + "imagetool.jar" + PS +
        //        getImagetoolHome() + FS + "lib" + FS + "* -Djava.util.logging.config.file=" +
        //        getImagetoolHome() + FS + "bin" + FS + "logging.properties com.oracle.weblogic.imagetool.cli.CLIDriver";
        imagetool = "java -cp \"" + getImagetoolHome() + FS + "lib" + FS + "*\" -Djava.util.logging.config.file=" +
                getImagetoolHome() + FS + "bin" + FS + "logging.properties com.oracle.weblogic.imagetool.cli.CLIDriver";

        logger.info("DEBUG: WLSIMG_BLDDIR=" + wlsImgBldDir);
        logger.info("DEBUG: WLSIMG_CACHEDIR=" + wlsImgCacheDir);
        logger.info("DEBUG: imagetool=" + imagetool);
    }

    protected static void setup() throws Exception {

        logger.info("Setting up the test ..." +
                "");
        // unzip the weblogic-image-tool/imagetool/target/imagetool-${VERSION}-SNAPSHOT.zip
        String command = "/bin/rm -rf " + getImagetoolHome();
        logger.info("Executing command: " + command);
        ExecCommand.exec(command);

        command = "/bin/unzip " + getTargetDir() + FS + imagetoolZipfile;
        logger.info("Executing command: " + command);
        ExecCommand.exec(command);

        command = "source " + getImagetoolHome() + FS + "bin" + FS + "setup.sh";
        logger.info("Executing command: " + command );
        ExecResult result = ExecCommand.exec(command);
    }

    protected static void cleanup() throws Exception {
        logger.info("cleaning up cache entries");
        logger.info("executing command: /bin/rm -rf " + wlsImgCacheDir);
        ExecCommand.exec("/bin/rm -rf " + wlsImgCacheDir);
        logger.info("executing command: /bin/mkdir " + wlsImgCacheDir);
        ExecCommand.exec("/bin/mkdir " + wlsImgCacheDir);
    }

    protected static void pullDockerImage() throws Exception {
        logger.info("Pulling OS base images from OCIR ...");
        String ocir_username = System.getenv("OCIR_USERNAME");
        String ocir_password = System.getenv("OCIR_PASSWORD");

        if(ocir_username == null || ocir_password == null) {
            throw new Exception("You need to set OCIR_USERNAME and OCIR_PASSWORD environment variable to pull images");
        }

        ExecCommand.exec("docker login " + OCIR_SERVER + " -u " + OCIR_TENENT + "/" + ocir_username +
                " -p " + ocir_password);
        ExecCommand.exec("docker pull " + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG);

        // verify the docker image is pulled
        ExecResult result = ExecCommand.exec("docker images | grep " + BASE_OS_IMG  + " | grep " +
                BASE_OS_IMG_TAG + "| wc -l");
        if(Integer.parseInt(result.stdout()) != 1) {
            throw new Exception("Base OS docker image is not pulled as expected");
        }
    }

    protected static void downloadInstallers(String... installers) throws Exception {
        // create the cache dir for downloading installers if not exists
        File cacheDir = new File(getInstallerCacheDir());
        if( !cacheDir.exists()) {
            cacheDir.mkdir();
        }

        // check the required installer is downloaded
        for(String installer : installers) {
            File installFile = new File(getInstallerCacheDir() + FS + installer);
            if(!installFile.exists()) {
                throw new Exception("Please download " + installer + " from oracle support site and put it in " +
                        getInstallerCacheDir());
            }
        }
    }

    protected static String getProjectRoot() {
        return projectRoot;
    }

    protected static String getTargetDir() {
        return getProjectRoot() + FS + "target";
    }

    protected static String getImagetoolHome() {
        return getProjectRoot() + FS + "imagetool-" + VERSION + "-SNAPSHOT";
    }

    protected static String getInstallerCacheDir() {
        return getProjectRoot() + FS + "caches";
    }

    protected static String getWDTResourcePath() {
        return getProjectRoot() + FS + "src" + FS + "test" + FS + "resources" + FS + "wdt";
    }

    protected void verifyResult(ExecResult result, String matchString) throws Exception {
        if(result.exitValue() != 0 || !result.stdout().contains(matchString)) {
            throw new Exception("verifying test result failed.");
        }
    }

    protected void verifyExitValue(ExecResult result, String command) throws Exception {
        if(result.exitValue() != 0) {
            logger.info(result.stderr());
            throw new Exception("executing the following command failed: " + command);
        }
    }

    protected void verifyDockerImages(String imageTag) throws Exception {
        // verify the docker image is created
        ExecResult result = ExecCommand.exec("docker images | grep imagetool | grep " + imageTag +
                "| wc -l");
        if(Integer.parseInt(result.stdout()) != 1) {
            throw new Exception("wls docker image is not created as expected");
        }
    }

    protected void logTestBegin(String testMethodName) throws Exception {
        logger.info("=======================================");
        logger.info("BEGIN test " + testMethodName + " ...");
    }

    protected void logTestEnd(String testMethodName) throws Exception {
        logger.info("SUCCESS - " + testMethodName);
        logger.info("=======================================");
    }

    protected ExecResult listItemsInCache() throws Exception {
        String command = imagetool + " cache listItems";
        return executeAndVerify(command, false);
    }

    protected ExecResult addInstallerToCache(String type, String version, String path) throws Exception {
        String command = imagetool + " cache addInstaller --type " + type + " --version " + version +
                " --path " + path;
        return executeAndVerify(command, false);
    }

    protected ExecResult addPatchToCache(String type, String patchId, String version, String path) throws Exception {
        String command = imagetool + " cache addPatch --type " + type + " --patchId " + patchId + "_" +
                version + " --path " + path;
        return executeAndVerify(command, false);
    }

    protected ExecResult addEntryToCache(String entryKey, String entryValue) throws Exception {
        String command = imagetool + " cache addEntry --key " + entryKey + " --value " + entryValue;
        return executeAndVerify(command, false);
    }

    protected ExecResult deleteEntryFromCache(String entryKey) throws Exception {
        String command = imagetool + " cache deleteEntry --key " + entryKey;
        return executeAndVerify(command, false);
    }

    protected ExecResult buildWDTArchive() throws Exception {
        logger.info("Building WDT archive ...");
        String command = "sh " + getWDTResourcePath() + FS + "build-archive.sh";
        return executeAndVerify(command, true);
    }

    private ExecResult executeAndVerify(String command, boolean isRedirectToOut) throws Exception {
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command, isRedirectToOut);
        verifyExitValue(result, command);
        logger.info(result.stdout());
        return result;
    }
}
