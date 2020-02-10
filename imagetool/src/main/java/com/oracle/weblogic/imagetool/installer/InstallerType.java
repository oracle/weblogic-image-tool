// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

/**
 * Supported installer types in the local cache.
 * Provides list of types to the CLI for the addInstaller type flag.
 */
public enum InstallerType {

    WLS("wls"),
    FMW("fmw"),
    SOA("soa"),
    OSB("osb"),
    OHS("ohs"),
    IDM("idm"),
    OAM("oam"),
    OUD("oud"),
    WCC("wcc"),
    WCP("wcp"),
    WCS("wcs"),
    JDK("jdk"),
    WDT("wdt");

    private String value;

    /**
     * Create an Enum value for the installer type.
     * @param value value is the first part of the key for the installer in the cache, and the filename of the
     *              response file
     */
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
