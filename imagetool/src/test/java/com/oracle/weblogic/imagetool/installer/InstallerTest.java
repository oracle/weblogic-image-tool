// Copyright (c) 2020, 2026, Oracle and/or its affiliates.
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
            AruProduct.FIT, AruProduct.OSS, AruProduct.FMW, AruProduct.JDEV, AruProduct.OPSS,
            AruProduct.OWSM, AruProduct.FMC, AruProduct.EM, AruProduct.UMS, AruProduct.OVD};
        assertEquals(Utils.toSet(list2), FmwInstallerType.FMW.products(),
            "FMW product list is incorrect or out of order");

        AruProduct[] list3 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMW_GLCM, AruProduct.FMWPLAT, AruProduct.JDBC,
            AruProduct.FIT, AruProduct.OSS, AruProduct.FMW, AruProduct.JDEV, AruProduct.OPSS,
            AruProduct.OWSM, AruProduct.FMC, AruProduct.EM, AruProduct.UMS, AruProduct.OVD, AruProduct.SOA};
        assertEquals(Utils.toSet(list3), FmwInstallerType.SOA.products(),
            "SOA product list is incorrect or out of order");

        AruProduct[] list4 = {AruProduct.WLS, AruProduct.COH, AruProduct.FMW_GLCM, AruProduct.FMWPLAT, AruProduct.JDBC,
            AruProduct.FIT, AruProduct.OSS, AruProduct.FMW, AruProduct.JDEV, AruProduct.OPSS,
            AruProduct.OWSM, AruProduct.FMC, AruProduct.EM, AruProduct.UMS, AruProduct.OVD, AruProduct.OAM,
            AruProduct.IMS};
        assertEquals(Utils.toSet(list4), FmwInstallerType.OAM.products(),
            "OAM product list is incorrect or out of order");

        AruProduct[] list5 = {AruProduct.OHS, AruProduct.OAM_WG, AruProduct.WLS, AruProduct.JDBC,
            AruProduct.FMWPLAT, AruProduct.OSS, AruProduct.FIT, AruProduct.FMW, AruProduct.JRF,
            AruProduct.FMW_GLCM};
        assertEquals(Utils.toSet(list5), FmwInstallerType.OHS.products(),
            "OHS product list is incorrect or out of order");
    }

    @Test
    void fromDistributionList() {
        assertEquals(FmwInstallerType.WLS, FmwInstallerType.fromDistributionList("WebLogic Server,OPatch"));
        assertEquals(FmwInstallerType.FMW, FmwInstallerType.fromDistributionList("WebLogic Server for FMW"));
        assertEquals(FmwInstallerType.SOA, FmwInstallerType.fromDistributionList(
            "WebLogic Server for FMW,BPM_SOA"));
        assertEquals(FmwInstallerType.SOA_OSB, FmwInstallerType.fromDistributionList(
            "WebLogic Server for FMW,BPM_SOA,ServiceBus"));
        assertEquals(FmwInstallerType.OUD_WLS, FmwInstallerType.fromDistributionList(
            "WebLogic Server for FMW,Oracle Unified Directory"));
        assertEquals(FmwInstallerType.IDM_WLS, FmwInstallerType.fromDistributionList(
            "Oracle Identity Management QuickStart"));
        assertNull(FmwInstallerType.fromDistributionList("OPatch"));
        assertNull(FmwInstallerType.fromDistributionList(""));
        assertNull(FmwInstallerType.fromDistributionList(null));
    }

    @Test
    void productMap() {
        assertEquals(FmwInstallerType.WLS, ProductMap.getInstallerType("WebLogic Server"));
        assertEquals(FmwInstallerType.FMW, ProductMap.getInstallerType("WebLogic Server for FMW"));
        assertEquals(FmwInstallerType.SOA, ProductMap.getInstallerType("BPM_SOA"));
        assertEquals(FmwInstallerType.OSB, ProductMap.getInstallerType("ServiceBus"));
        assertNull(ProductMap.getInstallerType("OPatch"));
    }
}
