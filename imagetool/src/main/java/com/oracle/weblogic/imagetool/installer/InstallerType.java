// Copyright (c) 2019, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

/**
 * Supported installer types in the local cache.
 * Provides list of types to the CLI for the addInstaller type flag.
 */
public enum InstallerType {

    WLSDEV("wlsdev"),
    WLSSLIM("wlsslim"),
    WLS("wls"),
    FMW("fmw"),
    SOA("soa"),
    OSB("osb"),
    B2B("b2b"),
    MFT("mft"),
    IDM("idm"),
    OAM("oam"),
    OHS("ohs"),
    DB19("db19"),
    OUD("oud"),
    OID("oid"),
    WCC("wcc"),
    WCP("wcp"),
    WCS("wcs"),
    JDK("jdk"),
    WDT("wdt"),
    ODI("odi");

    private final String value;

    /**
     * Create an Enum value for the installer type.
     * @param value value is the first part of the key for the installer in the cache, and the filename of the
     *              response file
     */
    InstallerType(String value) {
        this.value = value;
    }

    /**
     * Return the enum value from string.
     * @param value input value
     * @return InstallerType
     */
    public static InstallerType fromString(String value) {
        for (InstallerType installerType : InstallerType.values()) {
            if (installerType.toString().equalsIgnoreCase(value)) {
                return installerType;
            }
        }
        throw new IllegalArgumentException(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
