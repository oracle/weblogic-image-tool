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

    private static String projectRoot = "";
    protected static String wlsImgBldDir = "";
    protected static String wlsImgCacheDir = "";
    protected static String imagetool = "";
    private static String imagetoolZipfile = "";


    protected static void initialize() throws Exception {
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

        imagetool = "java -cp " + getImagetoolHome() + FS + "lib" + FS + "imagetool.jar" + PS +
                getImagetoolHome() + FS + "lib" + FS + "* -Djava.util.logging.config.file=" +
                getImagetoolHome() + FS + "bin" + FS + "logging.properties com.oracle.weblogic.imagetool.cli.CLIDriver";

        logger.info("DEBUG: WLSIMG_BLDDIR=" + wlsImgBldDir);
        logger.info("DEBUG: WLSIMG_CACHEDIR=" + wlsImgCacheDir);
        logger.info("DEBUG: imagetool=" + imagetool);
    }

    protected static void setup() throws Exception {

        // unzip the weblogic-image-tool/imagetool/target/imagetool-${VERSION}-SNAPSHOT.zip
        ExecCommand.exec("/bin/rm -rf " + getImagetoolHome());
        ExecCommand.exec("/bin/unzip " + getTargetDir() + FS + imagetoolZipfile);
        logger.info("running script " + getImagetoolHome() + FS + "bin" + FS + "setup.sh" );
        ExecResult result = ExecCommand.exec("source " + getImagetoolHome() + FS + "bin" + FS + "setup.sh");
        logger.info("DEBUG: running setup.sh ..." );
        logger.info(result.stderr());

        //result = ExecCommand.exec(imagetool);
        //logger.info("DEBUG: running imagetool");
        //logger.info(result.stdout());
    }

    protected  static String getProjectRoot() {
        return projectRoot;
    }

    protected  static String getTargetDir() {
        return getProjectRoot() + FS + "target";
    }

    protected  static String getImagetoolHome() {
        return getProjectRoot() + FS + "imagetool-" + VERSION + "-SNAPSHOT";
    }

    protected void verifyResult(ExecResult result, String matchString) throws Exception {
        if(result.exitValue() != 0 || !result.stdout().contains(matchString)) {
            throw new Exception("verifying test result failed.");
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
        logger.info("executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        logger.info(result.stdout());
        return result;
    }

    protected ExecResult addInstallerToCache(String type, String version, String path) throws Exception {
        String command = imagetool + " cache addInstaller --type " + type + " --version " + version +
                " --path " + path;
        logger.info("executing command: " + command);
        ExecResult result = ExecCommand.exec(command);
        logger.info(result.stdout());
        return result;
    }

}
