// Copyright (c) 2020. 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

public class AruException extends Exception {
    public AruException() {
        super();
    }

    public AruException(String message) {
        super(message);
    }

    public AruException(String message, Throwable thrown) {
        super(message, thrown);
    }
}
