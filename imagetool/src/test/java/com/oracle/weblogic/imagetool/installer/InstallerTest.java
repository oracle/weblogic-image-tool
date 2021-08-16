// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;

import com.oracle.weblogic.imagetool.aru.AruProduct;
import com.oracle.weblogic.imagetool.util.Utils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
class InstallerTest {
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
        AruProduct[] list1 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT};
        assertEquals(Utils.toSet(list1), FmwInstallerType.WLS.products(),
            "WLS product list is incorrect or out of order");

        AruProduct[] list2 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT, AruProduct.JRF, AruProduct.JDEV};
        assertEquals(Utils.toSet(list2), FmwInstallerType.FMW.products(),
            "FMW product list is incorrect or out of order");

        AruProduct[] list3 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT, AruProduct.JRF, AruProduct.JDEV,
            AruProduct.SOA};
        assertEquals(Utils.toSet(list3), FmwInstallerType.SOA.products(),
            "SOA product list is incorrect or out of order");
    }

    @Test
    void fmwInstallerFromValue() {
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromValue("FMW"),
            "fromValue FMW failed for FmwInstallerType");
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromValue("fmw"),
            "fromValue fmw failed for FmwInstallerType");
    }

    @Test
    void fromProductList() {
        assertEquals(FmwInstallerType.WLS, FmwInstallerType.fromProductList("WLS,COH,TOPLINK"));
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromProductList("INFRA,WLS,COH,TOPLINK"));
        assertEquals(FmwInstallerType.SOA_OSB, FmwInstallerType.fromProductList("INFRA,WLS,COH,TOPLINK,BPM,SOA,OSB"));
        // Ignore unsupported products, but keep as many products as possible that ARE known
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromProductList("INFRA,WLS,COH,TOPLINK,OIM"));
        assertNull(FmwInstallerType.fromProductList(""));
        assertNull(FmwInstallerType.fromProductList(null));
    }
}

