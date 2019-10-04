// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.util.Constants;

/**
 * An enum to represent installer type.
 */
public enum InstallerType {

    FMW(WLSInstallerType.FMW.toString()),
    JDK("jdk"),
    WDT("wdt"),
    WLS(WLSInstallerType.WLS.toString());

    private String value;

    InstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Get the Dockerfile build argument for this installer type.
     * @param location the location of the installer.
     * @return --build-arg and this installer type argument and location.
     */
    public List<String> getBuildArg(String location) {
        List<String> retVal = new ArrayList<>(2);
        retVal.add(Constants.BUILD_ARG);
        if (this == WLS || this == FMW) {
            retVal.add("WLS_PKG=" + location);
        } else if (this == JDK) {
            retVal.add("JAVA_PKG=" + location);
        } else {
            retVal.add("WDT_PKG=" + location);
        }
        return retVal;
    }

    /**
     * Create the installer type Enum from the String value.
     * @param value the installer type string, ignoring case.
     * @return the enum installer type.
     */
    public static InstallerType fromValue(String value) {
        for (InstallerType eachType : InstallerType.values()) {
            if (eachType.value.equalsIgnoreCase(value)) {
                return eachType;
            }
        }
        throw new IllegalArgumentException("argument " + value + " does not match any InstallerType");
    }
}
