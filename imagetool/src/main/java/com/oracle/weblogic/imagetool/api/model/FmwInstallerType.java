// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

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
    OAM(InstallerType.FMW, InstallerType.IDM),
    BI(InstallerType.FMW, InstallerType.BI),
    ODI(InstallerType.FMW, InstallerType.ODI),
    EDQ(InstallerType.FMW, InstallerType.EDQ),
    OHSSA(InstallerType.OHS),
    OHS(InstallerType.FMW, InstallerType.OHS),
    OIG(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.IDM),
    OUD(InstallerType.OUD),
    OUDSM(InstallerType.FMW, InstallerType.OUD),
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
