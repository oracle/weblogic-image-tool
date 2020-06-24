// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

public enum PackageManagerType {
    OS_DEFAULT,
    NONE,
    YUM,
    APTGET,
    APK,
    ZYPPER;

    /**
     * Returns the package manager that should be used to install software on a given Linux OS.
     * Defaults to YUM.  Known OS's include: ubuntu, debian, opensuse, centos, ol (Oracle Linux), and rhel.
     *
     * @param osID identifier for the OS, like ubuntu, debian, rhel, ol, ...
     * @return the package manager
     */
    public static PackageManagerType fromOperatingSystem(String osID) {
        if (osID == null) {
            return YUM;
        }

        osID = osID.replaceAll("[\"]", "");
        switch (osID) {
            case "ubuntu":
            case "debian":
                return APTGET;
            case "opensuse":
                return ZYPPER;
            case "centos":
            case "ol":
            case "rhel":
            default:
                return YUM;
        }
    }
}
