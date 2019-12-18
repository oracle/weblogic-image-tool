// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.util.Constants;

/**
 * Supported installer types in the local cache.
 * Provides list of types to the CLI for the addInstaller type flag.
 */
public enum InstallerType {

    //This ENUM MUST contain all types from FmwInstallerType in addition to JDK and WDT types
    WLS(FmwInstallerType.WLS.toString().toLowerCase()),
    FMW(FmwInstallerType.FMW.toString().toLowerCase()),
    SOA(FmwInstallerType.SOA.toString().toLowerCase()),
    OSB(FmwInstallerType.OSB.toString().toLowerCase()),
    JDK("jdk"),
    WDT("wdt");

    private String value;

    InstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
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
