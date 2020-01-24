// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

/**
 * Supported installer types in the local cache.
 * Provides list of types to the CLI for the addInstaller type flag.
 */
public enum InstallerType {

    WLS("wls", "WebLogic Server"),
    FMW("fmw", "Fusion Middleware Infrastructure"),
    SOA("soa", "SOA Suite"),
    OSB("osb", "Service Bus"),
    OHS("ohs", "Standalone HTTP Server (Managed independently of WebLogic server)"),
    IDM("idm", ""),
    OAM("oam", "Collocated Oracle Identity and Access Manager (Managed through WebLogic server)"),
    OUD("oud", "Installation for Oracle Unified Directory"),
    WCC("wcc", "WebCenter Content"),
    WCP("wcp", "WebCenter Portal"),
    WCS("wcs", "WebCenter Sites"),
    JDK("jdk", null), //JDK does not require a silent response file
    WDT("wdt", null); //WDT does not require a silent response file

    private String value;
    private String responseFileInstallType;

    /**
     * Create an Enum value for the installer type.
     * @param value value is the first part of the key for the installer in the cache, and the filename of the
     *              response file
     * @param installType used to populate the INSTALL_TYPE for the FMW silent install response file
     */
    InstallerType(String value, String installType) {
        this.value = value;
        this.responseFileInstallType = installType;
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

    /**
     * Get the value for the INSTALL_TYPE field of the FMW installer silent response file.
     * @return value for INSTALL_TYPE
     */
    public String getInstallTypeResponse() {
        return responseFileInstallType;
    }
}
