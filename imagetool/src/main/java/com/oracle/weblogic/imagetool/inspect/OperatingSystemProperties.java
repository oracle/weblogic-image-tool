// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.Properties;

import com.oracle.weblogic.imagetool.util.Utils;

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
        result.id = removeQuotes(imageProperties.getProperty("__OS__ID"));
        result.version = removeQuotes(imageProperties.getProperty("__OS__VERSION"));
        result.name = removeQuotes(imageProperties.getProperty("__OS__NAME"));
        return result;
    }

    private static String removeQuotes(String value) {
        if (Utils.isEmptyString(value)) {
            return value;
        }
        return value.replace("\"", "");
    }
}
