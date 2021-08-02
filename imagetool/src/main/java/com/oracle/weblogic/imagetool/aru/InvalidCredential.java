// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import com.oracle.weblogic.imagetool.util.Utils;

public class InvalidCredential extends AruException {
    public InvalidCredential() {
        super(Utils.getMessage("IMG-0022"));
    }
}
