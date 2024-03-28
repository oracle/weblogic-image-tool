// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class BuildPlatform {
    private static final LoggingFacade logger = LoggingFactory.getLogger(BuildPlatform.class);

    public static final String ARM64 = "linux/arm64";
    public static final String AMD64 = "linux/amd64";

    public static final String OS_ARCH;

    static {
        OS_ARCH = System.getProperty("os.arch");
        logger.fine("Local machine architecture is {0}", OS_ARCH);
    }

    private BuildPlatform() {
        // just static methods for now
    }

    /**
     * Get the build platform name using System property "os.arch".
     * @return name of the build platform, like linux/amd64.
     */
    public static String getPlatformName() {
        return getPlatformName(OS_ARCH);
    }

    /**
     * Get the build platform name using the provided architecture name.
     * @return name of the build platform, like linux/amd64.
     */
    public static String getPlatformName(String architecture) {
        logger.entering(architecture);
        String result;
        switch (architecture) {
            case "arm64":
            case "aarch64":
                result = ARM64;
                break;
            case "amd64":
            case "x86_64":
                result = AMD64;
                break;
            default:
                // this can occur when the JDK provides an unknown ID for the OS in the system property os.arch
                logger.warning("Unsupported architecture type {0}, defaulting to amd64");
                result = AMD64;
        }
        logger.exiting(result);
        return result;
    }
}
