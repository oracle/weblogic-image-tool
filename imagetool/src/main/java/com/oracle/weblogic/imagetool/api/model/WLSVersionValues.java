// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.util.ArrayList;
import java.util.Arrays;

import com.oracle.weblogic.imagetool.util.Constants;

public class WLSVersionValues extends ArrayList<String> {
    WLSVersionValues() {
        super(Arrays.asList(Constants.DEFAULT_WLS_VERSION, "12.2.1.2.0"));
    }
}
