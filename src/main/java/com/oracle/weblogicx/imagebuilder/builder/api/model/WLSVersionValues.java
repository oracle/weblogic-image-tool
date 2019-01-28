package com.oracle.weblogicx.imagebuilder.builder.api.model;

import java.util.ArrayList;
import java.util.Arrays;

import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_WLS_VERSION;

public class WLSVersionValues extends ArrayList<String> {
    WLSVersionValues() {
        super(Arrays.asList(DEFAULT_WLS_VERSION, "12.2.1.2.0"));
    }
}
