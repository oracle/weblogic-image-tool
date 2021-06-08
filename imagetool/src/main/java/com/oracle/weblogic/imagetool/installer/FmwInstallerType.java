// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.aru.AruProduct;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Supported Fusion Middleware installer sets.
 * Each installer set contains a list of ARU Products for resolving the product name and ARU ID,
 * as well as a list of installers needed to create the Oracle Home for that product.
 */
public enum FmwInstallerType {
    // data from https://updates.oracle.com/Orion/Services/metadata?table=aru_products

    // Oracle WebLogic Server
    WLS(Arrays.asList(AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT),
        InstallerType.WLS),
    WLSSLIM(Utils.list(WLS.products),
        InstallerType.WLSSLIM),
    WLSDEV(Utils.list(WLS.products),
        InstallerType.WLSDEV),

    // Oracle WebLogic Server Infrastructure (JRF)
    FMW(Utils.list(WLS.products, AruProduct.JRF, AruProduct.JDEV),
        InstallerType.FMW),
    // Oracle Service Bus
    OSB(Utils.list(FMW.products, AruProduct.OSB),
        InstallerType.FMW, InstallerType.OSB),
    // Oracle SOA Suite
    SOA(Utils.list(FMW.products, AruProduct.SOA),
        InstallerType.FMW, InstallerType.SOA),
    // Oracle SOA Suite (with Service Bus)
    SOA_OSB(Utils.list(FMW.products, AruProduct.SOA, AruProduct.OSB),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB),
    // Oracle SOA Suite (with Service Bus and B2B)
    SOA_OSB_B2B(Utils.list(FMW.products, AruProduct.SOA, AruProduct.OSB),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.B2B),
    // Oracle Managed File Transfer
    MFT(Utils.list(FMW.products, AruProduct.MFT),
        InstallerType.FMW, InstallerType.MFT),
    // Oracle Identity Manager
    IDM(Utils.list(FMW.products, AruProduct.IDM),
        InstallerType.FMW, InstallerType.IDM),
    // Oracle Identity Manager
    IDM_WLS(Collections.singletonList(AruProduct.IDM),
        InstallerType.IDM),
    // Oracle Access Manager
    OAM(Utils.list(FMW.products, AruProduct.OAM),
        InstallerType.FMW, InstallerType.OAM),
    // Oracle Identity Governance
    OIG(Utils.list(FMW.products, AruProduct.SOA, AruProduct.OSB, AruProduct.IDM),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.IDM),
    // Oracle Unified Directory
    OUD(Collections.singletonList(AruProduct.OUD),
        InstallerType.OUD),
    // Oracle Unified Directory
    OUD_WLS(Utils.list(FMW.products, AruProduct.OUD),
        InstallerType.FMW, InstallerType.OUD),
    // Oracle WebCenter Content
    WCC(Utils.list(FMW.products, AruProduct.WCC),
        InstallerType.FMW, InstallerType.WCC),
    // Oracle WebCenter Portal
    WCP(Utils.list(FMW.products, AruProduct.WCP),
        InstallerType.FMW, InstallerType.WCP),
    // Oracle WebCenter Sites
    WCS(Utils.list(FMW.products, AruProduct.WCS),
        InstallerType.FMW, InstallerType.WCS)
    ;

    private final InstallerType[] installers;
    private final List<AruProduct> products;
    FmwInstallerType(List<AruProduct> products, InstallerType... installers) {
        this.installers = installers;
        this.products = products;
    }

    public List<InstallerType> installerList() {
        return Arrays.asList(installers);
    }

    public String installerListString() {
        return Arrays.stream(installers).map(Object::toString).collect(Collectors.joining(", "));
    }

    public List<AruProduct> products() {
        return products;
    }

    /**
     * Create the FMW installer type Enum from the String value.
     * @param value the installer type string, ignoring case.
     * @return the enum installer type.
     */
    public static FmwInstallerType fromValue(String value) {
        for (FmwInstallerType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(Utils.getMessage("IMG-0080", value));
    }

    private static final List<FmwInstallerType> weblogicServerTypes = Arrays.asList(WLS, WLSDEV, WLSSLIM);

    /**
     * Return a list of all WebLogic Server types (not JRF types).
     * @return list of WLS enum types.
     */
    public static boolean isBaseWeblogicServer(FmwInstallerType value) {
        return weblogicServerTypes.contains(value);
    }
}
