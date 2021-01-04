// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

public class UpdateCommand extends ImageToolCommand {

    private String fromImage;
    private String tag;
    private String patches;

    public UpdateCommand() {
        super("update");
    }

    public UpdateCommand fromImage(String value) {
        fromImage = value;
        return this;
    }

    public UpdateCommand tag(String value) {
        tag = value;
        return this;
    }

    public UpdateCommand tag(String name, String version) {
        return tag(name + ":" + version);
    }

    public UpdateCommand patches(String... values) {
        patches = String.join(",", values);
        return this;
    }

    /**
     * Generate the command using the provided command line options.
     * @return the imagetool command as a string suitable for running in ProcessBuilder
     */
    public String build() {
        return super.build()
            + field("--fromImage", fromImage)
            + field("--tag", tag)
            + field("--patches", patches);
    }
}
