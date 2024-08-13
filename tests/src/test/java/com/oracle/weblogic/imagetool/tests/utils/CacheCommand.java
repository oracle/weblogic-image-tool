// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.nio.file.Path;

public class CacheCommand extends ImageToolCommand {
    private String version;
    private String type;

    // cache flags
    private boolean listItems;
    private boolean addInstaller;
    private boolean addPatch;
    private boolean addEntry;
    private boolean deleteEntry;
    private String path;
    private String patchId;
    private String key;
    private String value;


    public CacheCommand() {
        super("cache");
    }

    public CacheCommand listItems(boolean value) {
        listItems = value;
        return this;
    }

    public CacheCommand addInstaller(boolean value) {
        addInstaller = value;
        return this;
    }

    public CacheCommand addPatch(boolean value) {
        addPatch = value;
        return this;
    }


    public CacheCommand addEntry(boolean value) {
        addEntry = value;
        return this;
    }


    public CacheCommand deleteEntry(boolean value) {
        deleteEntry = value;
        return this;
    }

    public CacheCommand path(Path value) {
        path = value.toString();
        return this;
    }

    public CacheCommand key(String value) {
        key = value;
        return this;
    }

    public CacheCommand value(Path value) {
        this.value = value.toString();
        return this;
    }

    public CacheCommand patchId(String value) {
        patchId = value;
        return this;
    }

    public CacheCommand patchId(String bug, String version) {
        return patchId(bug + "_" + version);
    }

    public CacheCommand version(String value) {
        version = value;
        return this;
    }

    public CacheCommand type(String value) {
        type = value;
        return this;
    }

    /**
     * Generate the command using the provided command line options.
     * @return the imagetool command as a string suitable for running in ProcessBuilder
     */
    @Override
    public String build() {
        return super.build()
            + field("listItems", listItems)
            + field("addInstaller", addInstaller)
            + field("addPatch", addPatch)
            + field("addEntry", addEntry)
            + field("deleteEntry", deleteEntry)
            + field("--key", key)
            + field("--value", value)
            + field("--version", version)
            + field("--type", type)
            + field("--path", path)
            + field("--patchId", patchId);
    }

}
