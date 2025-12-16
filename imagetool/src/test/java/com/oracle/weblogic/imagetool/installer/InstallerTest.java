// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;

import com.oracle.weblogic.imagetool.aru.AruProduct;
import com.oracle.weblogic.imagetool.util.Utils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class InstallerTest {
    @Test
    void fmwInstallerTypeStringTest() {
        assertEquals("fmw, soa, osb", FmwInstallerType.SOA_OSB.installerListString("12.2.1.4.0"),
            "string list of installer types for SOA_OSB is wrong");
    }

    @Test
    void fmwInstallerTypeListTest() {
        assertEquals(Arrays.asList(InstallerType.FMW, InstallerType.OSB),
            FmwInstallerType.OSB.installerList("12.2.1.4.0"), "installer list for OSB is wrong");

        assertEquals(Arrays.asList(InstallerType.FMW, InstallerType.SOA, InstallerType.OSB),
            FmwInstallerType.SOA_OSB.installerList("12.2.1.4.0"), "installer list for OSB is wrong");

        assertTrue(FmwInstallerType.OHS.installerList("12.2.1.4.0").contains(InstallerType.DB19));
        assertFalse(FmwInstallerType.OHS.installerList("14.1.2.0.0").contains(InstallerType.DB19),
            "Only OHS 12.2.1.4 requires the DB19 installer (patch)");
    }

    @Test
    void fmwInstallerProductIds() {
        AruProduct[] list1 = {AruProduct.WLS, AruProduct.FIT, AruProduct.JDBC, AruProduct.OSS, AruProduct.OWSM,
            AruProduct.COH, AruProduct.FMWPLAT, AruProduct.FMW_GLCM};
        assertEquals(Utils.toSet(list1), FmwInstallerType.WLS.products(),
            "WLS product list is incorrect or out of order");

        AruProduct[] list2 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMW_GLCM, AruProduct.FMWPLAT, AruProduct.JDBC,
            AruProduct.FIT, AruProduct.OSS, AruProduct.JRF, AruProduct.JDEV, AruProduct.OPSS, AruProduct.OWSM};
        assertEquals(Utils.toSet(list2), FmwInstallerType.FMW.products(),
            "FMW product list is incorrect or out of order");

        AruProduct[] list3 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMW_GLCM, AruProduct.FMWPLAT, AruProduct.JDBC,
            AruProduct.FIT, AruProduct.OSS, AruProduct.JRF, AruProduct.JDEV, AruProduct.OPSS, AruProduct.OWSM,
            AruProduct.SOA};
        assertEquals(Utils.toSet(list3), FmwInstallerType.SOA.products(),
            "SOA product list is incorrect or out of order");
    }

    @Test
    @Tag("failing")
    void fromProductList() {
        final String WLS_PRODUCTS = "WLS,COH,TOPLINK,JDBC,FIT,OSS";
        final String FMW_PRODUCTS = WLS_PRODUCTS + ",INFRA,OPSS,OWSM";
        assertEquals(FmwInstallerType.WLS, FmwInstallerType.fromProductList(WLS_PRODUCTS));
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromProductList(FMW_PRODUCTS));
        assertEquals(FmwInstallerType.SOA_OSB, FmwInstallerType.fromProductList(FMW_PRODUCTS + ",BPM,SOA,OSB"));
        // Ignore unsupported products, but keep as many products as possible that ARE known
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromProductList(FMW_PRODUCTS + ",OIM"));
        assertNull(FmwInstallerType.fromProductList(""));
        assertNull(FmwInstallerType.fromProductList(null));
    }
}

