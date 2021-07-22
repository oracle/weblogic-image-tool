// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.Properties;

public class OperatingSystemProperties {
    private String id;
    private String version;
    private String name;

    public String id() {
        return id;
    }

    public String version() {
        return version;
    }

    public String name() {
        return name;
    }

    /**
     * Using the properties obtained from the image, extract the OS properties prefixed with __OS__.
     * @param imageProperties properties returned from the image inspection
     * @return the OS property values as an object
     */
    public static OperatingSystemProperties getOperatingSystemProperties(Properties imageProperties) {
        OperatingSystemProperties result = new OperatingSystemProperties();
        result.id = imageProperties.getProperty("__OS__ID").replace("\"", "");
        result.version = imageProperties.getProperty("__OS__VERSION").replace("\"", "");
        result.name = imageProperties.getProperty("__OS__NAME").replace("\"", "");
        return result;
    }
}
