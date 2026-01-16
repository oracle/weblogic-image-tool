// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CreateAuxCommand extends ImageToolCommand {
    private String fromImage;
    private String tag;

    // WDT flags
    private String wdtVersion;
    private String wdtModel;
    private String wdtArchive;
    private String wdtVariables;
    private String wdtDomainHome;
    private boolean wdtModelOnly;
    private String platform;
    private boolean load;
    private boolean push;

    public CreateAuxCommand() {
        super("createAuxImage");
    }

    public CreateAuxCommand fromImage(String value) {
        fromImage = value;
        return this;
    }

    public CreateAuxCommand fromImage(String name, String version) {
        return fromImage(name + ":" + version);
    }

    public CreateAuxCommand tag(String value) {
        tag = value;
        return this;
    }

    public CreateAuxCommand wdtVersion(String value) {
        wdtVersion = value;
        return this;
    }

    public CreateAuxCommand load(boolean value) {
        load = value;
        return this;
    }

    public CreateAuxCommand push(boolean value) {
        push = value;
        return this;
    }


    public CreateAuxCommand wdtModel(Path... values) {
        wdtModel = Arrays.stream(values).map(Path::toString).collect(Collectors.joining(","));
        return this;
    }

    public CreateAuxCommand wdtArchive(Path value) {
        wdtArchive = value.toString();
        return this;
    }

    public CreateAuxCommand wdtVariables(Path value) {
        wdtVariables = value.toString();
        return this;
    }

    public CreateAuxCommand wdtDomainHome(String value) {
        wdtDomainHome = value;
        return this;
    }

    public CreateAuxCommand wdtModelOnly(boolean value) {
        wdtModelOnly = value;
        return this;
    }

    public CreateAuxCommand platform(String value) {
        platform = value;
        return this;
    }

    /**
     * Generate the command using the provided command line options.
     * @return the imagetool command as a string suitable for running in ProcessBuilder
     */
    @Override
    public String build() {
        return super.build()
            + field("--fromImage", fromImage)
            + field("--tag", tag)
            + field("--wdtVersion", wdtVersion)
            + field("--wdtModel", wdtModel)
            + field("--wdtArchive", wdtArchive)
            + field("--wdtVariables", wdtVariables)
            + field("--wdtDomainHome", wdtDomainHome)
            + field("--platform", platform)
            + field("--load", load)
            + field("--push", push)
            + field("--wdtModelOnly", wdtModelOnly);
    }
}
