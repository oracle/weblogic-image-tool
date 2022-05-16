// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    WLS(Utils.toSet(AruProduct.WLS, AruProduct.COH, AruProduct.FMWPLAT, AruProduct.FIT, AruProduct.JDBC),
        InstallerType.WLS),
    WLSSLIM(Utils.toSet(WLS.products),
        InstallerType.WLSSLIM),
    WLSDEV(Utils.toSet(WLS.products),
        InstallerType.WLSDEV),

    // Oracle WebLogic Server Infrastructure (JRF)
    FMW(Utils.toSet(WLS.products, AruProduct.JRF, AruProduct.JDEV, AruProduct.OPSS, AruProduct.OWSM),
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
    OAM(Utils.toSet(FMW.products, AruProduct.OAM),
        InstallerType.FMW, InstallerType.OAM),
    // Oracle Identity Governance
    OIG(Utils.toSet(FMW.products, AruProduct.SOA, AruProduct.OSB, AruProduct.IDM),
        InstallerType.FMW, InstallerType.SOA, InstallerType.OSB, InstallerType.IDM),
    // Oracle Unified Directory
    OUD(Collections.singleton(AruProduct.OUD),
        InstallerType.OUD),
    // Oracle Unified Directory
    OUD_WLS(Utils.toSet(FMW.products, AruProduct.OUD),
        InstallerType.FMW, InstallerType.OUD),
    // Oracle Internet Directory
    OID(Utils.toSet(FMW.products, AruProduct.OID),
        InstallerType.FMW, InstallerType.OID),
    // Oracle WebCenter Content
    WCC(Utils.toSet(FMW.products, AruProduct.WCC),
        InstallerType.FMW, InstallerType.WCC),
    // Oracle WebCenter Portal
    WCP(Utils.toSet(FMW.products, AruProduct.WCP),
        InstallerType.FMW, InstallerType.WCP),
    // Oracle WebCenter Sites
    WCS(Utils.toSet(FMW.products, AruProduct.WCS),
        InstallerType.FMW, InstallerType.WCS)
    ;

    private final InstallerType[] installers;
    private final Set<AruProduct> products;
    FmwInstallerType(Set<AruProduct> products, InstallerType... installers) {
        this.installers = installers;
        this.products = products;
    }

    public List<InstallerType> installerList() {
        return Arrays.asList(installers);
    }

    public String installerListString() {
        return Arrays.stream(installers).map(Object::toString).collect(Collectors.joining(", "));
    }

    public Set<AruProduct> products() {
        return products;
    }

    private static final List<FmwInstallerType> weblogicServerTypes = Arrays.asList(WLS, WLSDEV, WLSSLIM);

    private static final LoggingFacade logger = LoggingFactory.getLogger(FmwInstallerType.class);

    /**
     * Return a list of all WebLogic Server types (not JRF types).
     * @return list of WLS enum types.
     */
    public static boolean isBaseWeblogicServer(FmwInstallerType value) {
        return weblogicServerTypes.contains(value);
    }

    /**
     * Derive the FmwInstallerType from a list of product families.
     * These product families are found in inventory/registry.xml.
     * @param products a comma-separated list of product families
     * @return the best match for the list of product families
     */
    public static FmwInstallerType fromProductList(String products) {
        logger.entering(products);
        if (Utils.isEmptyString(products)) {
            return null;
        }

        // create a set from the comma-separated list
        Set<AruProduct> productSet = Stream.of(products.split(","))
            .map(e -> "INFRA".equals(e) ? "JRF" : e) // map -> replaces any occurrence of INFRA with JRF
            .filter(AruProduct::isKnownAruProduct) // drop any products that cannot be patched individually (or unknown)
            .map(AruProduct::valueOf) // convert String to AruProduct enum
            .collect(Collectors.toSet());

        logger.finer("Derived product set {0} from {1}", productSet, products);

        for (FmwInstallerType type : values()) {
            // Use the product set to compare products, but remove products that CIE does not include in registry.xml
            Set<AruProduct> aruProducts = type.products().stream()
                .filter(e -> !AruProduct.FMWPLAT.equals(e)) // never shows up on installed product family
                .filter(e -> !AruProduct.JDEV.equals(e)) // never shows up on installed product family
                .collect(Collectors.toSet());

            if (aruProducts.equals(productSet)) {
                logger.exiting(type);
                return type;
            }
        }
        logger.exiting(WLS);
        return WLS;
    }
}
