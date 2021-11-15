// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

public class InvalidPatchIdFormatException extends Exception {
    public InvalidPatchIdFormatException(String patchId, String expectedFormat) {
        super(Utils.getMessage("IMG-0004", patchId, expectedFormat));
    }
}
