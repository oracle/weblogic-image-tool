// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.cli.menu.KubernetesTarget;

public class CreateCommand extends ImageToolCommand {
    private String version;
    private String type;

    private String jdkVersion;
    private String fromImage;
    private String tag;
    private boolean latestPsu;
    private boolean recommendedPatches;
    private String user;
    private String passwordEnv;
    private String patches;
    private String additionalBuildCommands;
    private String kubernetesTarget;

    // WDT flags
    private String wdtVersion;
    private String wdtModel;
    private String wdtArchive;
    private String wdtVariables;
    private String wdtDomainHome;
    private String wdtDomainType;
    private boolean wdtRunRcu;
    private boolean wdtModelOnly;
    private String platform;
    private String commonName;

    public CreateCommand() {
        super("create");
    }

    public CreateCommand version(String value) {
        version = value;
        return this;
    }

    public CreateCommand jdkVersion(String value) {
        jdkVersion = value;
        return this;
    }

    public CreateCommand fromImage(String value) {
        fromImage = value;
        return this;
    }

    public CreateCommand fromImage(String name, String version) {
        return fromImage(name + ":" + version);
    }

    public CreateCommand tag(String value) {
        tag = value;
        return this;
    }

    public CreateCommand type(String value) {
        type = value;
        return this;
    }

    public CreateCommand latestPsu(boolean value) {
        latestPsu = value;
        return this;
    }

    public CreateCommand recommendedPatches(boolean value) {
        recommendedPatches = value;
        return this;
    }

    public CreateCommand user(String value) {
        user = value;
        return this;
    }

    public CreateCommand passwordEnv(String value) {
        passwordEnv = value;
        return this;
    }

    public CreateCommand patches(String... values) {
        patches = String.join(",", values);
        return this;
    }

    public CreateCommand additionalBuildCommands(Path value) {
        additionalBuildCommands = value.toString();
        return this;
    }

    public CreateCommand target(KubernetesTarget value) {
        kubernetesTarget = value.toString();
        return this;
    }

    public CreateCommand wdtVersion(String value) {
        wdtVersion = value;
        return this;
    }

    public CreateCommand wdtModel(Path... values) {
        wdtModel = Arrays.stream(values).map(Path::toString).collect(Collectors.joining(","));
        return this;
    }

    public CreateCommand wdtArchive(Path value) {
        wdtArchive = value.toString();
        return this;
    }

    public CreateCommand wdtVariables(Path value) {
        wdtVariables = value.toString();
        return this;
    }

    public CreateCommand wdtDomainHome(String value) {
        wdtDomainHome = value;
        return this;
    }

    public CreateCommand wdtDomainType(String value) {
        wdtDomainType = value;
        return this;
    }

    public CreateCommand wdtRunRcu(boolean value) {
        wdtRunRcu = value;
        return this;
    }

    public CreateCommand wdtModelOnly(boolean value) {
        wdtModelOnly = value;
        return this;
    }

    public CreateCommand platform(String value) {
        platform = value;
        return this;
    }

    public CreateCommand commonName(String value) {
        commonName = value;
        return this;
    }


    /**
     * Generate the command using the provided command line options.
     * @return the imagetool command as a string suitable for running in ProcessBuilder
     */
    @Override
    public String build() {
        return super.build()
            + field("--version", version)
            + field("--jdkVersion", jdkVersion)
            + field("--fromImage", fromImage)
            + field("--tag", tag)
            + field("--type", type)
            + field("--latestPSU", latestPsu)
            + field("--recommendedPatches", recommendedPatches)
            + field("--user", user)
            + field("--passwordEnv", passwordEnv)
            + field("--patches", patches)
            + field("--additionalBuildCommands", additionalBuildCommands)
            + field("--target", kubernetesTarget)
            + field("--wdtVersion", wdtVersion)
            + field("--wdtModel", wdtModel)
            + field("--wdtArchive", wdtArchive)
            + field("--wdtVariables", wdtVariables)
            + field("--wdtDomainHome", wdtDomainHome)
            + field("--wdtDomainType", wdtDomainType)
            + field("--wdtRunRCU", wdtRunRcu)
            + field("--platform", platform)
            + field("--commonName", commonName)
            + field("--wdtModelOnly", wdtModelOnly);

    }
}
