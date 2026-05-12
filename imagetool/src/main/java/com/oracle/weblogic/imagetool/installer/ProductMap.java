// Copyright (c) 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Map registry.xml distribution names to installer families.
 */
public class ProductMap implements Iterable<String> {
    static Map<String, FmwInstallerType> products;

    static {
        products = new LinkedHashMap<>();

        products.put("WebLogic Server", FmwInstallerType.WLS);
        products.put("WebLogic Server for FMW", FmwInstallerType.FMW);
        products.put("Oracle HTTP Server", FmwInstallerType.OHS);
        products.put("ServiceBus", FmwInstallerType.OSB);
        products.put("BPM_SOA", FmwInstallerType.SOA);
        products.put("MFT", FmwInstallerType.MFT);
        products.put("WebCenterContent", FmwInstallerType.WCC);
        products.put("WebCenterPortal", FmwInstallerType.WCP);
        products.put("WebCenterSites", FmwInstallerType.WCS);
        products.put("Oracle Identity Management", FmwInstallerType.IDM);
        products.put("Oracle Identity Management QuickStart", FmwInstallerType.IDM);
        products.put("Oracle Internet Directory", FmwInstallerType.OID);
        products.put("Oracle Data Integrator", FmwInstallerType.ODI);
        products.put("Oracle Unified Directory", FmwInstallerType.OUD);
    }

    public static FmwInstallerType getInstallerType(String distribution) {
        return products.get(distribution);
    }

    private ProductMap() {
    }

    @Override
    public @NotNull Iterator<String> iterator() {
        return products.keySet().iterator();
    }
}
