// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

public class RebaseCommand extends ImageToolCommand {
    private String targetImage;
    private String sourceImage;
    private String tag;

    public RebaseCommand() {
        super("rebase");
    }

    public RebaseCommand targetImage(String value) {
        targetImage = value;
        return this;
    }

    public RebaseCommand targetImage(String name, String version) {
        return targetImage(name + ":" + version);
    }

    public RebaseCommand sourceImage(String value) {
        sourceImage = value;
        return this;
    }

    public RebaseCommand sourceImage(String name, String version) {
        return sourceImage(name + ":" + version);
    }

    public RebaseCommand tag(String value) {
        tag = value;
        return this;
    }

    /**
     * Generate the command using the provided command line options.
     * @return the imagetool command as a string suitable for running in ProcessBuilder
     */
    public String build() {
        return super.build()
            + field("--targetImage", targetImage)
            + field("--sourceImage", sourceImage)
            + field("--tag", tag);
    }
}
