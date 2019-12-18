// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;

public class WLSInstallHelper {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WLSInstallHelper.class);

    /**
     * Set the docker options for build if fromImage parameter is present.
     * @param fromImage  image tag
     * @param dockerfileOptions docker options
     * @param tmpDir temporary directory
     * @throws Exception thrown by getBaseImageProperties
     */
    public static void copyOptionsFromImage(String fromImage, DockerfileOptions dockerfileOptions, String tmpDir)
        throws Exception {

        if (fromImage != null && !fromImage.isEmpty()) {
            logger.finer("IMG-0002", fromImage);
            dockerfileOptions.setBaseImage(fromImage);

            Utils.copyResourceAsFile("/probe-env/test-create-env.sh",
                tmpDir + File.separator + "test-env.sh", true);

            Properties baseImageProperties = Utils.getBaseImageProperties(fromImage, tmpDir);

            if (baseImageProperties.getProperty("WLS_VERSION", null) != null) {
                throw new IllegalArgumentException(Utils.getMessage("IMG-0038", fromImage,
                    baseImageProperties.getProperty("ORACLE_HOME")));
            }

            String existingJavaHome = baseImageProperties.getProperty("JAVA_HOME", null);
            if (existingJavaHome != null) {
                dockerfileOptions.disableJavaInstall(existingJavaHome);
                logger.info("IMG-0000", existingJavaHome);
            }

            String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
            if (!Utils.isEmptyString(pkgMgr)) {
                dockerfileOptions.setPackageInstaller(pkgMgr);
            }
        } else {
            dockerfileOptions.setPackageInstaller(Constants.YUM);
        }
    }

    /**
     * Copies response files required for wls install to the tmp directory which provides docker build context.
     *
     * @param dirPath directory to copy to
     * @throws IOException in case of error
     */
    public static void copyResponseFilesToDir(String dirPath, String installerResponseFile) throws IOException {
        if (installerResponseFile != null && Files.isRegularFile(Paths.get(installerResponseFile))) {
            logger.fine("IMG-0005", installerResponseFile);
            Path target = Paths.get(dirPath, "wls.rsp");
            Files.copy(Paths.get(installerResponseFile), target);
        } else {
            final String responseFile = "/response-files/wls.rsp";
            logger.fine("IMG-0005", responseFile);
            Utils.copyResourceAsFile(responseFile, dirPath, false);
        }

        Utils.copyResourceAsFile("/response-files/oraInst.loc", dirPath, false);
    }

    /**
     * Return a list of basic installers.
     *
     * @param initialList  An initial non-null list of installers
     * @param installerType installer type
     * @param installerVersion installer version
     * @param jdkVersion jdkVersion of the installer
     * @param dockerfileOptions  non null docker options
     * @return list of installers
     */
    public static  List<CachedFile> getBasicInstallers(List<CachedFile> initialList,
                                                       String installerType, String installerVersion,
                                                       String jdkVersion, DockerfileOptions dockerfileOptions) {
        logger.finer("IMG-0001", installerType, installerVersion);
        initialList.add(new CachedFile(InstallerType.fromValue(installerType),
            installerVersion));
        if (dockerfileOptions.installJava()) {
            logger.finer("IMG-0001", InstallerType.JDK, jdkVersion);
            initialList.add(new CachedFile(InstallerType.JDK, jdkVersion));
        }
        logger.exiting(initialList.size());
        return initialList;

    }
}
