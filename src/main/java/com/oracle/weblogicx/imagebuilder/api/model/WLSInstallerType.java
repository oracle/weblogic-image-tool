/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/

package com.oracle.weblogicx.imagebuilder.api.model;

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

    public static WLSInstallerType fromValue(String value) {
        for (WLSInstallerType eachType : WLSInstallerType.values()) {
            if (eachType.value.equalsIgnoreCase(value)) {
                return eachType;
            }
        }
        throw new IllegalArgumentException("argument " + value + " does not match any WLSInstallerType");
    }
}
