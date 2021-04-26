// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;

import com.oracle.weblogic.imagetool.aru.AruProduct;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
public class InstallerTest {
    @Test
    void fmwInstallerTypeStringTest() {
        assertEquals("fmw, soa, osb", FmwInstallerType.SOA_OSB.installerListString(),
            "string list of installer types for SOA_OSB is wrong");
    }

    @Test
    void fmwInstallerTypeListTest() {
        assertEquals(Arrays.asList(InstallerType.FMW, InstallerType.OSB),
            FmwInstallerType.OSB.installerList(), "installer list for OSB is wrong");

        assertEquals(Arrays.asList(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB),
            FmwInstallerType.SOA_OSB.installerList(), "installer list for OSB is wrong");
    }

    @Test
    void fmwInstallerProductIds() {
        assertEquals(Arrays.asList(AruProduct.WLS, AruProduct.COH), FmwInstallerType.WLS.products(),
            "WLS product list is incorrect or out of order");
        assertEquals(Arrays.asList(AruProduct.WLS, AruProduct.COH, AruProduct.JRF, AruProduct.FMWPLAT, AruProduct.JDEV),
            FmwInstallerType.FMW.products(), "FMW product list is incorrect or out of order");
        assertEquals(Arrays.asList(AruProduct.WLS, AruProduct.COH, AruProduct.JRF, AruProduct.FMWPLAT, AruProduct.JDEV,
            AruProduct.SOA), FmwInstallerType.SOA.products(), "SOA product list is incorrect or out of order");
    }

    @Test
    void fmwInstallerFromValue() {
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromValue("FMW"),
            "fromValue FMW failed for FmwInstallerType");
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromValue("fmw"),
            "fromValue fmw failed for FmwInstallerType");
    }
}

