// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

public class NoPatchesFoundException extends AruException {
    public NoPatchesFoundException(String msg) {
        super(msg);
    }

    public NoPatchesFoundException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
