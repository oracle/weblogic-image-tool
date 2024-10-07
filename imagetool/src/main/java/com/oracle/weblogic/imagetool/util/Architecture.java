// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

/**
 * Platform is a menu option that sets the OS and Architecture for the target build.
 */
public enum Architecture {
    ARM64(541, "arm64", "linux/arm64", "aarch64"), // Linux ARM 64-bit
    AMD64(226, "amd64", "linux/amd64", "x86_64"); // Linux AMD 64-bit

    private static final LoggingFacade logger = LoggingFactory.getLogger(Architecture.class);

    private final int aruPlatform;
    private final String[] acceptableNames;


    /**
     * Create Architecture definitions.
     * @param aruPlatform the ARU code for a given OS/architecture.
     * @param acceptableNames the acceptable strings from a command line input that can be mapped to this Enum.
     */
    Architecture(int aruPlatform, String... acceptableNames) {
        this.aruPlatform = aruPlatform;
        this.acceptableNames = acceptableNames;
    }

    /**
     * Get the ARU platform code.
     * @return the ARU platform code.
     */
    public int getAruPlatform() {
        return aruPlatform;
    }

    public List<String> getAcceptableNames() {
        return Collections.unmodifiableList(Arrays.asList(acceptableNames));
    }

    @Override
    public String toString() {
        return acceptableNames[0];
    }

    /**
     * Given a string value from the user, get the Architecture enum.
     *
     * @param value string value to map to an Architecture Enum.
     * @return the Architecture type found or null if not found.
     */
    public static Architecture fromString(String value) {
        for (Architecture arch: values()) {
            for (String name: arch.acceptableNames) {
                if (name.equalsIgnoreCase(value)) {
                    return arch;
                }
            }
        }
        logger.warning("IMG-0121", value);
        return AMD64;
    }

    /**
     * Given an int value from ARU for the platform, get the Architecture enum.
     *
     * @param platform the ARU platform.
     * @return the Architecture type found or null if not found.
     */
    public static Architecture fromAruPlatform(int platform) {
        for (Architecture arch: values()) {
            if (arch.aruPlatform == platform) {
                return arch;
            }
        }
        return null;
    }

    /**
     * Get the architecture of the local operating system.
     * @return name of the build platform, like linux/amd64.
     */
    public static Architecture getLocalArchitecture() {
        return fromString(System.getProperty("os.arch"));
    }
}
