// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.oracle.weblogic.imagetool.tests.utils.ExecCommand;
import com.oracle.weblogic.imagetool.tests.utils.ExecResult;

public class BaseTest {

    protected static final Logger logger = Logger.getLogger(ITImagetool.class.getName());
    protected static final String FS = File.separator;
    protected static final String BASE_OS_IMG = "phx.ocir.io/weblogick8s/oraclelinux";
    protected static final String BASE_OS_IMG_TAG = "7-4imagetooltest";
    protected static final String ORACLE_DB_IMG = "phx.ocir.io/weblogick8s/database/enterprise";
    protected static final String ORACLE_DB_IMG_TAG = "12.2.0.1-slim";
    protected static String dbContainerName = "";
    private static String projectRoot = "";
    protected static final String wlsImgBldDir = getEnvironmentProperty("WLSIMG_BLDDIR");
    protected static final String wlsImgCacheDir = getEnvironmentProperty("WLSIMG_CACHEDIR");
    protected static String imagetool;
    private static final String IMAGETOOLDIR = "imagetool";
    // STAGING_DIR - directory where JDK and other installers are pre-staged before testing
    private static final String STAGING_DIR = getEnvironmentProperty("STAGING_DIR");
    protected static String build_tag = "";
    protected static final String WDT_MODEL1 = "simple-topology1.yaml";

    /**
     * Get the named property from system environment or Java system property.
     * If the property is defined in the Environment, that value will take precedence over
     * Java properties.
     *
     * @param name the name of the environment variable, or Java property
     * @return the value defined in the env or system property
     */
    public static String getEnvironmentProperty(String name) {
        String result = System.getenv(name);
        if (result == null || result.isEmpty()) {
            result = System.getProperty(name);
        }
        return result;
    }

    protected static void validateEnvironmentSettings() {
        logger.info("Initializing the tests ...");

        projectRoot = System.getProperty("user.dir");

        List<String> missingSettings = new ArrayList<>();
        if (wlsImgBldDir == null || wlsImgBldDir.isEmpty()) {
            missingSettings.add("WLSIMG_BLDDIR");
        }

        if (wlsImgCacheDir == null || wlsImgCacheDir.isEmpty()) {
            missingSettings.add("WLSIMG_CACHEDIR");
        }

        if (STAGING_DIR == null || STAGING_DIR.isEmpty()) {
            missingSettings.add("STAGING_DIR");
        }

        if (missingSettings.size() > 0) {
            String error = String.join(", ", missingSettings)
                + " must be set as a system property or ENV variable";
            throw new IllegalArgumentException(error);
        }

        imagetool = getImagetoolHome() + FS + "bin" + FS + "imagetool.sh";

        // get the build tag from Jenkins build environment variable BUILD_TAG
        build_tag = System.getenv("BUILD_TAG");
        if (build_tag != null) {
            build_tag = build_tag.toLowerCase();
        } else {
            build_tag = "imagetool-itest";
        }
        dbContainerName = "InfraDB4" + build_tag;
        logger.info("build_tag = " + build_tag);
        logger.info("WLSIMG_BLDDIR = " + wlsImgBldDir);
        logger.info("WLSIMG_CACHEDIR = " + wlsImgCacheDir);
        logger.info("imagetool script = " + imagetool);
    }

    protected static void setup() {

        logger.info("Setting up the test environment ...");

        if (!(new File(wlsImgBldDir)).exists()) {
            logger.info(wlsImgBldDir + " does not exist, creating it");
            if (!(new File(wlsImgBldDir)).mkdir()) {
                throw new IllegalStateException("Unable to create build directory " + wlsImgBldDir);
            }
        }
    }

    protected static void cleanup() throws Exception {
        logger.info("cleaning up the test environment ...");

        // clean up the db container
        String command = "docker rm -f -v " + dbContainerName;
        executeNoVerify(command);

        // clean up the images created in the tests
        command = "docker rmi -f $(docker images -q '" + build_tag + "' | uniq)";
        executeNoVerify(command);
    }

    protected static void pullBaseOsDockerImage() throws Exception {
        logger.info("Pulling OS base images from OCIR ...");
        pullDockerImage(BASE_OS_IMG, BASE_OS_IMG_TAG);
    }

    protected static void pullOracleDbDockerImage() throws Exception {
        logger.info("Pulling Oracle DB image from OCIR ...");
        pullDockerImage(ORACLE_DB_IMG, ORACLE_DB_IMG_TAG);
    }

    static void verifyStagedFiles(String... installers) {
        // determine if any of the required installers are missing from the stage directory
        List<String> missingInstallers = new ArrayList<>();
        for (String installer : installers) {
            File installFile = new File(getStagingDir() + FS + installer);
            if (!installFile.exists()) {
                missingInstallers.add(installer);
            }
        }
        if (missingInstallers.size() > 0) {
            String error = "Could not find these installers in the staging directory: " + getStagingDir() + "\n   ";
            error += String.join("\n   ", missingInstallers);
            throw new IllegalStateException(error);
        }
    }

    protected static String getProjectRoot() {
        return projectRoot;
    }

    protected static String getTargetDir() {
        return getProjectRoot() + FS + "target";
    }

    protected static String getImagetoolHome() {
        return getTargetDir() + FS + IMAGETOOLDIR;
    }

    protected static String getStagingDir() {
        return STAGING_DIR;
    }

    protected static String getWdtResourcePath() {
        return getProjectRoot() + FS + "src" + FS + "test" + FS + "resources" + FS + "wdt";
    }

    protected static String getAbcResourcePath() {
        return getProjectRoot() + FS + "src" + FS + "test" + FS + "resources" + FS + "additionalBuildCommands";
    }

    protected static void executeNoVerify(String command) throws Exception {
        logger.info("executing command: " + command);
        ExecCommand.exec(command);
    }

    protected void verifyResult(ExecResult result, String matchString) throws Exception {
        if (result.exitValue() != 0 || !result.stdout().contains(matchString)) {
            throw new Exception("verifying test result failed.");
        }
    }

    protected void verifyExitValue(ExecResult result, String command) throws Exception {
        if (result.exitValue() != 0) {
            logger.info("ERROR: result.exitValue=" + result.exitValue());
            throw new Exception("executing the following command failed: " + command);
        }
    }

    protected void verifyDockerImages(String imageTag) throws Exception {
        // verify the docker image is created
        ExecResult result = ExecCommand.exec("docker images | grep " + build_tag + " | grep " + imageTag
            + "| wc -l");
        if (Integer.parseInt(result.stdout().trim()) != 1) {
            throw new Exception("wls docker image is not created as expected");
        }
    }

    protected void verifyFileInImage(String imagename, String filename, String expectedContent) throws Exception {
        logger.info("verifying the file content in image");
        String command = "docker run --rm " + imagename + " bash -c 'cat " + filename + "'";
        logger.info("executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        if (!result.stdout().contains(expectedContent)) {
            throw new Exception("The image " + imagename + " does not have the expected file content: "
                + expectedContent);
        }
    }

    protected void verifyLabelInImage(String imagename, String label) throws Exception {
        ExecResult result = ExecCommand.exec("docker inspect --format '{{ index .Config.Labels}}' "
            + imagename);
        if (!result.stdout().contains(label)) {
            throw new Exception("The image " + imagename + " does not contain the expected label " + label);
        }
    }

    protected void logTestBegin(String testMethodName) {
        logger.info("=======================================");
        logger.info("BEGIN test " + testMethodName + " ...");
    }

    protected void logTestEnd(String testMethodName) {
        logger.info("SUCCESS - " + testMethodName);
        logger.info("=======================================");
    }

    protected ExecResult listItemsInCache() throws Exception {
        String command = imagetool + " cache listItems";
        return executeAndVerify(command, true);
    }

    protected ExecResult addInstallerToCache(String type, String version, String path) throws Exception {
        String command = imagetool + " cache addInstaller --type " + type + " --version " + version
            + " --path " + path;
        return executeAndVerify(command, false);
    }

    protected ExecResult addPatchToCache(String patchId, String version, String path) throws Exception {
        String command = imagetool + " cache addPatch --patchId " + patchId + "_" + version + " --path " + path;
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

    protected ExecResult buildWdtArchive() throws Exception {
        logger.info("Building WDT archive ...");
        String command = "sh " + getWdtResourcePath() + FS + "build-archive.sh";
        return executeAndVerify(command, true);
    }

    protected void createDBContainer() throws Exception {
        logger.info("Creating an Oracle db docker container ...");
        String command = "docker rm -f " + dbContainerName;
        ExecCommand.exec(command);
        command = "docker run -d --name " + dbContainerName + " --env=\"DB_PDB=InfraPDB1\""
            + " --env=\"DB_DOMAIN=us.oracle.com\" --env=\"DB_BUNDLE=basic\" " + ORACLE_DB_IMG + ":"
            + ORACLE_DB_IMG_TAG;
        logger.info("executing command: " + command);
        ExecCommand.exec(command);

        // wait for the db is ready
        command = "docker ps | grep " + dbContainerName;
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
        ExecResult result = ExecCommand.exec(command);
        verifyExitValue(result, command);
        logger.info(result.stdout());
        return result;
    }

    private static void pullDockerImage(String imagename, String imagetag) throws Exception {

        String pullCommand = "docker pull " + imagename + ":" + imagetag;
        logger.info(pullCommand);
        ExecResult pullResult = ExecCommand.exec(pullCommand);

        // verify the docker image is pulled
        ExecResult result = ExecCommand.exec("docker images | grep " + imagename  + " | grep "
            + imagetag + "| wc -l");
        String resultString = result.stdout();
        if (Integer.parseInt(resultString.trim()) != 1) {
            throw new Exception("docker image " + imagename + ":" + imagetag + " is not pulled as expected."
                    + " Expected 1 image, found " + resultString);
        }
    }

    private static void checkCmdInLoop(String cmd, String matchStr) throws Exception {
        final int maxIterations = 50;

        int i = 0;
        while (i < maxIterations) {
            ExecResult result = ExecCommand.exec(cmd);

            // pod might not have been created or if created loop till condition
            if (result.exitValue() != 0
                    || (result.exitValue() == 0 && !result.stdout().contains(matchStr))) {
                // check for last iteration
                if (i == (maxIterations - 1)) {
                    throw new RuntimeException(
                            "FAILURE: " + cmd + " does not return the expected string " + matchStr + ", exiting!");
                }
                final int waitTime = 5;
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
