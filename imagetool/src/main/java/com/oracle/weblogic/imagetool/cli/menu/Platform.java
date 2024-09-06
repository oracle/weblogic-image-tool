// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

/**
 * Platform is a menu option that sets the OS and Architecture for the target build.
 */
public enum Platform {
    ARM64(541, "linux/arm64", "arm64"), // Linux ARM 64-bit
    AMD64(226, "linux/amd64", "amd64"); // Linux AMD 64-bit

    private final int aruPlatform;
    private final String[] acceptableNames;

    /**
     * Create Platform definitions.
     * @param aruPlatform the ARU code for a given OS/architecture.
     * @param acceptableNames the acceptable strings from a command line input that can be mapped to a Platform.
     */
    Platform(int aruPlatform, String... acceptableNames) {
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

    /**
     * Given a string value from the user, get the Enum type.
     *
     * @param value string value to map to a Platform Enum.
     * @return the Platform type found or null if not found.
     */
    public Platform fromString(String value) {
        for (Platform p: values()) {
            for (String name: p.acceptableNames) {
                if (name.equalsIgnoreCase(value)) {
                    return p;
                }
            }
        }
        return null;
    }
}
