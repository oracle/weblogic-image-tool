// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.util.Properties;

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
}
