// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

/**
 * For a given list of patches returned from ARU, the version requested was not found.
 */
public class ReleaseNotFoundException extends AruException {
    /**
     * The product/version combination was not found in the release table on ARU.
     */
    public ReleaseNotFoundException(String message) {
        super(message);
    }
}
