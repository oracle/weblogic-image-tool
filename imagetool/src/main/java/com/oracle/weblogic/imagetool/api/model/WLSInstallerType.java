// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

/**
 * Supported installer type. WebLogic Server and FMW Infrastructure.
 */
public enum WLSInstallerType {
    WLS("wls"),
    FMW("fmw");

    private String value;

    WLSInstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Create the WLS installer type Enum from the String value.
     * @param value the installer type string, ignoring case.
     * @return the enum installer type.
     */
    public static WLSInstallerType fromValue(String value) {
        for (WLSInstallerType eachType : WLSInstallerType.values()) {
            if (eachType.value.equalsIgnoreCase(value)) {
                return eachType;
            }
        }
        throw new IllegalArgumentException("argument " + value + " does not match any WLSInstallerType");
    }
}
