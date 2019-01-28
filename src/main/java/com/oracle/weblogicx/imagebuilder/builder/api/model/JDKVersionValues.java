package com.oracle.weblogicx.imagebuilder.builder.api.model;

import java.util.ArrayList;
import java.util.Arrays;

import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_JDK_VERSION;

public class JDKVersionValues extends ArrayList<String> {
    JDKVersionValues() {
        super(Arrays.asList(DEFAULT_JDK_VERSION, "9"));
    }
}
