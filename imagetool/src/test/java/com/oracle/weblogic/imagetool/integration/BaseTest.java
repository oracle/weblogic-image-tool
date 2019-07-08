// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.integration;

import com.oracle.weblogic.imagetool.integration.utils.ExecCommand;
import com.oracle.weblogic.imagetool.integration.utils.ExecResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class BaseTest {

    protected static final Logger logger = Logger.getLogger(ITImagetool.class.getName());
    protected static final String VERSION = "1.0.1";
    protected static final String PS = File.pathSeparator;
    protected static final String FS = File.separator;
    private static final String OCIR_SERVER = "phx.ocir.io";
    private static final String OCIR_TENENT = "weblogick8s";
    private static final String OCR_SERVER = "container-registry.oracle.com";
    protected static final String BASE_OS_IMG = "phx.ocir.io/weblogick8s/oraclelinux";
    protected static final String BASE_OS_IMG_TAG = "7-4imagetooltest";
    protected static final String ORACLE_DB_IMG = "container-registry.oracle.com/database/enterprise";
    protected static final String ORACLE_DB_IMG_TAG = "12.2.0.1-slim";
    private static final String DB_CONTAINER_NAME = "InfraDB";
    private static String projectRoot = "";
    protected static String wlsImgBldDir = "";
    protected static String wlsImgCacheDir = "";
    protected static String imagetool = "";
    private static String imagetoolZipfile = "";
    private static int maxIterations = 50;
    private static int waitTime = 5;

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

        imagetool = "java -cp \"" + getImagetoolHome() + FS + "lib" + FS + "*\" -Djava.util.logging.config.file=" +
                getImagetoolHome() + FS + "bin" + FS + "logging.properties com.oracle.weblogic.imagetool.cli.CLIDriver";

        logger.info("DEBUG: WLSIMG_BLDDIR=" + wlsImgBldDir);
        logger.info("DEBUG: WLSIMG_CACHEDIR=" + wlsImgCacheDir);
        logger.info("DEBUG: imagetool=" + imagetool);
    }

    protected static void setup() throws Exception {

        logger.info("Setting up the test environment ...");
        String command = "/bin/rm -rf " + getImagetoolHome();
        executeNoVerify(command);

        // unzip the weblogic-image-tool/imagetool/target/imagetool-${VERSION}-SNAPSHOT.zip
        command = "/bin/unzip " + getTargetDir() + FS + imagetoolZipfile;
        executeNoVerify(command);

        command = "source " + getImagetoolHome() + FS + "bin" + FS + "setup.sh";
        executeNoVerify(command);
    }

    protected static void cleanup() throws Exception {
        logger.info("cleaning up the test environment ...");
        String command = "/bin/rm -rf " + wlsImgCacheDir;
        executeNoVerify(command);

        command = "/bin/mkdir " + wlsImgCacheDir;
        executeNoVerify(command);

        // clean up the docker images
        command = "docker stop " + DB_CONTAINER_NAME;
        executeNoVerify(command);
        command = "docker rmi -f " + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG + " " + ORACLE_DB_IMG + ":" +
                ORACLE_DB_IMG_TAG;
        executeNoVerify(command);

        // clean up the possible left over wlsimgbuilder_temp*
        command = "/bin/rm -rf " + wlsImgBldDir + FS + "wlsimgbuilder_temp*";
        executeNoVerify(command);
    }

    protected static void pullBaseOSDockerImage() throws Exception {
        logger.info("Pulling OS base images from OCIR ...");
        String ocir_username = System.getenv("OCIR_USERNAME");
        String ocir_password = System.getenv("OCIR_PASSWORD");

        if(ocir_username == null || ocir_password == null) {
            throw new Exception("You need to set OCIR_USERNAME and OCIR_PASSWORD environment variable to pull base" +
                    " OS image " + BASE_OS_IMG + ":" + BASE_OS_IMG_TAG);
        }

        pullDockerImage(OCIR_SERVER, OCIR_TENENT + "/" + ocir_username , ocir_password, BASE_OS_IMG,
                BASE_OS_IMG_TAG);
    }

    protected static void pullOracleDBDockerImage() throws Exception {
        logger.info("Pulling Oracle DB image from OCR ...");
        String ocr_username = System.getenv("OCR_USERNAME");
        String ocr_password = System.getenv("OCR_PASSWORD");

        if(ocr_username == null || ocr_password == null) {
            throw new Exception("You need to set OCR_USERNAME and OCR_PASSWORD environment variable to pull DB " +
                    "image " + ORACLE_DB_IMG + ":" + ORACLE_DB_IMG_TAG);
        }

        pullDockerImage(OCR_SERVER, ocr_username, ocr_password, ORACLE_DB_IMG, ORACLE_DB_IMG_TAG);
    }

    protected static void downloadInstallers(String... installers) throws Exception {
        // create the cache dir for downloading installers if not exists
        File cacheDir = new File(getInstallerCacheDir());
        if( !cacheDir.exists()) {
            cacheDir.mkdir();
        }

        boolean missingInstaller = false;
        StringBuffer errorMsg = new StringBuffer();
        errorMsg.append("The test installers are missing. Please download: \n");
        // check the required installer is downloaded
        for(String installer : installers) {
            File installFile = new File(getInstallerCacheDir() + FS + installer);
            if(!installFile.exists()) {
                missingInstaller = true;
                errorMsg.append( "    " + installer + "\n");
            }
        }
        errorMsg.append("and put them in " + getInstallerCacheDir());
        if(missingInstaller) {
            throw new Exception(errorMsg.toString());
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

    protected static void executeNoVerify(String command) throws Exception {
        logger.info("executing command: " + command);
        ExecCommand.exec(command);
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
        if(Integer.parseInt(result.stdout().trim()) != 1) {
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

    protected void createDBContainer() throws Exception {
        logger.info("Creating an Oracle db docker container ...");
        String command = "docker rm -f " + DB_CONTAINER_NAME;
        ExecCommand.exec(command);
        command = "docker run -d --name " + DB_CONTAINER_NAME + " -p 1521:1521 -p 5500:5500 --env=\"DB_PDB=InfraPDB1\"" +
                " --env=\"DB_DOMAIN=us.oracle.com\" --env=\"DB_BUNDLE=basic\" " + ORACLE_DB_IMG + ":" +
                ORACLE_DB_IMG_TAG;
        ExecCommand.exec(command);

        // wait for the db is ready
        command = "docker ps | grep " + DB_CONTAINER_NAME;
        checkCmdInLoop(command, "healthy");
    }

    protected static void replaceStringInFile(String filename, String originalString, String newString)
            throws Exception {
        Path path = Paths.get(filename);

        String content = new String(Files.readAllBytes(path));
        content = content.replaceAll(originalString, newString);
        Files.write(path, content.getBytes());
    }

    private ExecResult executeAndVerify(String command, boolean isRedirectToOut) throws Exception {
        logger.info("Executing command: " + command);
        ExecResult result = ExecCommand.exec(command, isRedirectToOut);
        verifyExitValue(result, command);
        logger.info(result.stdout());
        return result;
    }

    private static void pullDockerImage(String repoServer, String username, String password,
                                        String imagename, String imagetag) throws Exception {

        ExecCommand.exec("docker login " + repoServer + " -u " + username +
                " -p " + password);
        ExecCommand.exec("docker pull " + imagename + ":" + imagetag);

        // verify the docker image is pulled
        ExecResult result = ExecCommand.exec("docker images | grep " + imagename  + " | grep " +
                imagetag + "| wc -l");
        String resultString = result.stdout();
        if(Integer.parseInt(resultString.trim()) != 1) {
            throw new Exception("docker image " + imagename + ":" + imagetag + " is not pulled as expected."
                    + " Expected 1 image, found " + resultString);
        }
    }

    private static void checkCmdInLoop(String cmd, String matchStr)
            throws Exception {
        int i = 0;
        while (i < maxIterations) {
            ExecResult result = ExecCommand.exec(cmd);

            // pod might not have been created or if created loop till condition
            if (result.exitValue() != 0
                    || (result.exitValue() == 0 && !result.stdout().contains(matchStr))) {
                logger.info("Output for " + cmd + "\n" + result.stdout() + "\n " + result.stderr());
                // check for last iteration
                if (i == (maxIterations - 1)) {
                    throw new RuntimeException(
                            "FAILURE: " + cmd + " does not return the expected string " + matchStr + ", exiting!");
                }
                logger.info(
                        "Waiting for the expected String " + matchStr
                                + ": Ite ["
                                + i
                                + "/"
                                + maxIterations
                                + "], sleeping "
                                + waitTime
                                + " seconds more");

                Thread.sleep(waitTime * 1000);
                i++;
            } else {
                logger.info("get the expected String " + matchStr);
                break;
            }
        }
    }
}
