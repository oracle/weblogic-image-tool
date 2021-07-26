// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UpdateCommand extends ImageToolCommand {

    private String fromImage;
    private String tag;
    private String patches;

    // WDT flags
    private String wdtVersion;
    private String wdtModel;
    private String wdtArchive;
    private String wdtVariables;
    private boolean wdtModelOnly;

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

    public UpdateCommand patches(String... values) {
        patches = String.join(",", values);
        return this;
    }

    public UpdateCommand wdtVersion(String value) {
        wdtVersion = value;
        return this;
    }

    public UpdateCommand wdtModel(Path... values) {
        wdtModel = Arrays.stream(values).map(Path::toString).collect(Collectors.joining(","));
        return this;
    }

    public UpdateCommand wdtArchive(Path value) {
        wdtArchive = value.toString();
        return this;
    }

    public UpdateCommand wdtVariables(Path value) {
        wdtVariables = value.toString();
        return this;
    }

    public UpdateCommand wdtModelOnly(boolean value) {
        wdtModelOnly = value;
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
            + field("--patches", patches)
            + field("--wdtVersion", wdtVersion)
            + field("--wdtModel", wdtModel)
            + field("--wdtArchive", wdtArchive)
            + field("--wdtVariables", wdtVariables)
            + field("--wdtModelOnly", wdtModelOnly);
    }
}
