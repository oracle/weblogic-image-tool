// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

/**
 * Class that holds the results of using java to exec a command (i.e. exit value, stdout and stderr)
 */
public class ExecResult {
    private int exitValue;
    private String stdout;

    public ExecResult(int exitValue, String stdout) {
        this.exitValue = exitValue;
        this.stdout = stdout;
    }

    public int exitValue() {
        return this.exitValue;
    }

    public String stdout() {
        return this.stdout;
    }

}
