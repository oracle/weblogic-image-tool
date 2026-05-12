// Copyright (c) 2019, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.aru.AruProduct;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Supported Fusion Middleware installer sets.
 * Each installer set contains a list of ARU Products for resolving the product name and ARU ID,
 * as well as a list of installers needed to create the Oracle Home for that product.
 */
public enum FmwInstallerType {
    // data from https://updates.oracle.com/Orion/Services/metadata?table=aru_products

    // Oracle WebLogic Server
    WLS(Utils.toSet(AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT, AruProduct.FIT, AruProduct.JDBC,
        AruProduct.FMW_GLCM, AruProduct.OSS, AruProduct.OWSM),
        InstallerType.WLS),
    // WLS: Added OSS for a special patching issue for 12.2.1.4 JDBC fix
    // WLS: Added OWSM because osdt jars are included in the WLS install
    WLSSLIM(Utils.toSet(WLS.products),
        InstallerType.WLSSLIM),
    WLSDEV(Utils.toSet(WLS.products),
        InstallerType.WLSDEV),

    // Oracle WebLogic Server Infrastructure (JRF)
    FMW(Utils.toSet(WLS.products, AruProduct.FMW, AruProduct.JDEV, AruProduct.OPSS, AruProduct.OWSM,
        AruProduct.EM, AruProduct.FMC, AruProduct.UMS, AruProduct.OVD),
        InstallerType.FMW),
    // Oracle Service Bus
    OSB(Utils.toSet(FMW.products, AruProduct.OSB),
        InstallerType.FMW, InstallerType.OSB),
    // Oracle SOA Suite
    SOA(Utils.toSet(FMW.products, AruProduct.SOA),
        InstallerType.FMW, InstallerType.SOA),
    // Oracle SOA Suite (with Service Bus)
    SOA_OSB(Utils.toSet(FMW.products, AruProduct.SOA, AruProduct.OSB),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB),
    // Oracle SOA Suite (with Service Bus and B2B)
    SOA_OSB_B2B(Utils.toSet(FMW.products, AruProduct.SOA, AruProduct.OSB),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.B2B),
    // Oracle Managed File Transfer
    MFT(Utils.toSet(FMW.products, AruProduct.MFT),
        InstallerType.FMW, InstallerType.MFT),
    // Oracle Identity Manager
    IDM(Utils.toSet(FMW.products, AruProduct.IDM),
        InstallerType.FMW, InstallerType.IDM),
    // Oracle Identity Manager
    IDM_WLS(Collections.singleton(AruProduct.IDM),
        InstallerType.IDM),
    // Oracle Access Manager
    OAM(Utils.toSet(FMW.products, AruProduct.OAM, AruProduct.IMS),
        InstallerType.FMW, InstallerType.OAM),
    // Oracle Identity Governance
    OIG(Utils.toSet(FMW.products, AruProduct.SOA, AruProduct.OSB, AruProduct.IDM),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.IDM),
    // Oracle Unified Directory
    OUD(Utils.toSet(AruProduct.OUD, AruProduct.IMS),
        InstallerType.OUD),
    // Oracle Unified Directory
    OUD_WLS(Utils.toSet(FMW.products, AruProduct.OUD, AruProduct.IMS),
        InstallerType.FMW, InstallerType.OUD),
    // Oracle Internet Directory
    OID(Utils.toSet(FMW.products, AruProduct.OID, AruProduct.IMS),
        InstallerType.FMW, InstallerType.OID),
    // Oracle WebCenter Content
    WCC(Utils.toSet(FMW.products, AruProduct.WCC),
        InstallerType.FMW, InstallerType.WCC),
    // Oracle WebCenter Portal
    WCP(Utils.toSet(FMW.products, AruProduct.WCP),
        InstallerType.FMW, InstallerType.WCP),
    // Oracle WebCenter Sites
    WCS(Utils.toSet(FMW.products, AruProduct.WCS),
        InstallerType.FMW, InstallerType.WCS),
    OHS(Utils.toSet(AruProduct.OHS, AruProduct.OAM_WG, AruProduct.WLS, AruProduct.JDBC, AruProduct.FMWPLAT,
        AruProduct.OSS, AruProduct.FIT, AruProduct.FMW, AruProduct.JRF, AruProduct.FMW_GLCM),
        InstallerType.OHS) {
        @Override
        public List<InstallerType> installerList(String version) {
            List<InstallerType> ohsInstallers = new ArrayList<>(Arrays.asList(OHS.installers));
            if (version.equals("12.2.1.4.0")) {
                ohsInstallers.add(InstallerType.DB19);
            }
            return ohsInstallers;
        }
    },
    ODI(Collections.singleton(AruProduct.ODI),
        InstallerType.ODI)
    ;

    private final InstallerType[] installers;
    private final Set<AruProduct> products;

    FmwInstallerType(Set<AruProduct> products, InstallerType... installers) {
        this.installers = installers;
        this.products = products;
    }

    @SuppressWarnings("unused") // version parameter is needed in the OHS override function
    public List<InstallerType> installerList(String version) {
        return Arrays.asList(installers);
    }

    public String installerListString(String version) {
        return installerList(version).stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public Set<AruProduct> products() {
        return products;
    }

    private static final List<FmwInstallerType> weblogicServerTypes = Arrays.asList(WLS, WLSDEV, WLSSLIM);
    private static final LoggingFacade logger = LoggingFactory.getLogger(FmwInstallerType.class);

    /**
     * Returns true if the installer type is a WLS installer, WLS, WLSDEV, or WLSSLIM.
     * @return true if the installer is a WLS installer type.
     */
    public static boolean isBaseWeblogicServer(FmwInstallerType value) {
        return weblogicServerTypes.contains(value);
    }

    /**
     * Derive the FmwInstallerType from a list of registry.xml distributions.
     * These distribution names are found in inventory/registry.xml under /registry/distributions/distribution.
     * @param distributions a comma-separated list of registry.xml distribution names
     * @return the best match for the list of distributions, or null if no known distributions are present
     */
    public static FmwInstallerType fromDistributionList(String distributions) {
        logger.entering(distributions);
        if (Utils.isEmptyString(distributions)) {
            return null;
        }

        Set<FmwInstallerType> distributionSet = Stream.of(distributions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(ProductMap::getInstallerType)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(FmwInstallerType.class)));

        logger.finer("Derived distribution set {0} from {1}", distributionSet, distributions);

        FmwInstallerType installerType = findInstallerType(distributionSet);
        logger.exiting(installerType);
        return installerType;
    }

    private static FmwInstallerType findInstallerType(Set<FmwInstallerType> distributionSet) {
        if (distributionSet.isEmpty()) {
            return null;
        }
        if (distributionSet.contains(SOA) && distributionSet.contains(OSB)) {
            return SOA_OSB;
        }
        if (distributionSet.contains(OUD)) {
            return distributionSet.contains(FMW) ? OUD_WLS : OUD;
        }
        if (distributionSet.contains(IDM)) {
            return distributionSet.contains(FMW) ? IDM : IDM_WLS;
        }

        List<FmwInstallerType> resolutionOrder = Arrays.asList(
            OAM, OID, WCC, WCP, WCS, OHS, MFT, ODI, SOA, OSB, FMW, WLS
        );
        for (FmwInstallerType type : resolutionOrder) {
            if (distributionSet.contains(type)) {
                return type;
            }
        }
        return null;
    }
}
