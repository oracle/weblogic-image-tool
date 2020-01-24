// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;
import java.util.List;

/**
 * Supported Fusion Middleware installer sets.
 */
public enum FmwInstallerType {
    WLS(InstallerType.WLS), //WebLogic Server
    FMW(InstallerType.FMW), //WebLogic Server Infrastructure (JRF)
    OSB(InstallerType.FMW, InstallerType.OSB),  //Service Bus
    SOA(InstallerType.FMW, InstallerType.SOA),  //SOA Suite
    SOAOSB(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB),
    IDM(InstallerType.FMW, InstallerType.IDM),
    OAM(InstallerType.FMW, InstallerType.OAM), //Identity and Access Manager
    OHS(InstallerType.OHS),
    OHS_WLS(InstallerType.FMW, InstallerType.OHS),
    OIG(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.IDM),
    OUD(InstallerType.OUD), //Oracle Unified Directory
    OUD_WLS(InstallerType.FMW, InstallerType.OUD), //Oracle Unified Directory
    WCC(InstallerType.FMW, InstallerType.WCC), //Web Center Content
    WCP(InstallerType.FMW, InstallerType.WCP), //Web Center Portal
    WCS(InstallerType.FMW, InstallerType.WCS)  //Web Center Sites
    ;

    private InstallerType[] installers;
    FmwInstallerType(InstallerType... installers) {
        this.installers = installers;
    }

    public List<InstallerType> getInstallerList() {
        return Arrays.asList(installers);
    }
}
