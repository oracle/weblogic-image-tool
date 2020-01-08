// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.util.Arrays;
import java.util.List;

/**
 * Supported Fusion Middleware installer sets.
 */
public enum FmwInstallerType {
    WLS(InstallerType.WLS),
    FMW(InstallerType.FMW),
    OSB(InstallerType.FMW, InstallerType.OSB),
    SOA(InstallerType.FMW, InstallerType.SOA),
    SOAOSB(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB);

    private InstallerType[] installers;
    FmwInstallerType(InstallerType... installers) {
        this.installers = installers;
    }

    public List<InstallerType> getInstallerList() {
        return Arrays.asList(installers);
    }
}
