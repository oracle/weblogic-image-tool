// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helps build the imagetool command for running Image Tool in integration tests.
 */
public class ImageToolCommand {
    private String operation;

    protected  ImageToolCommand(String operation) {
        this.operation = operation;
    }

    protected String field(String cli, String value) {
        if (value != null && !value.isEmpty()) {
            return " " + cli + " " + value;
        } else {
            return "";
        }
    }

    protected String field(String cli, boolean value) {
        if (value) {
            return " " + cli;
        } else {
            return "";
        }
    }

    public String build() {
        Path imagetoolPath = Paths.get("target","imagetool", "bin", "imagetool.sh");
        return imagetoolPath.toString() + " " + operation;
    }
}
