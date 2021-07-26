// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

public class InvalidPatchNumberException extends AruException {
    /**
     * Specified patch number is invalid for this use case.
     *
     * @param msg         error message
     */
    public InvalidPatchNumberException(String msg) {
        super(msg);
    }
}
