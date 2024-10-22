// Copyright (c) 2020, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.Arrays;

/**
 * ARU product codes for patching.
 */
public enum AruProduct {
    WLS("15991", "Oracle WebLogic Server"),
    COH("13964", "Oracle Coherence"),
    JRF("10120", "Oracle Fusion Middleware"),
    FMWPLAT("27638", "FMW Platform"),
    OSB("16011", "Oracle Service Bus"),
    SOA("12745", "Oracle SOA Suite"),
    MFT("27538", "Oracle Managed File Transfer"),
    IDM("18391", "Oracle Identity Manager"),
    OAM("18388", "Oracle Access Manager"),
    OHS("10300", "Oracle HTTP Server"),
    OUD("19748", "Oracle Unified Directory"),
    OID("10040", "Oracle Internet Directory"),
    WCC("13946", "Oracle WebCenter Content"),
    WCP("15224", "Oracle WebCenter Portal"),
    WCS("20995", "Oracle WebCenter Sites"),
    JDEV("11281", "Oracle JDeveloper"),
    OPSS("16606", "Oracle Platform Security Service"),
    OWSM("12787", "Oracle Webservices Manager"),
    JDBC("9512", "Oracle JDBC for Fusion Middleware"),
    FIT("33256", "Middleware Common Libraries and Tools"),
    ODI("13724", "Oracle Data Integrator"),
    OSS("16609", "Oracle Security Service"),
    OAM_WG("18388", "Oracle Access Manager Web Gates"),
    FMW_GLCM("31939", "Oracle Global Lifecycle Management FMW Installer")
    ;

    private final String productId;
    private final String description;

    AruProduct(String productId, String description) {
        this.productId = productId;
        this.description = description;
    }

    public String productId() {
        return productId;
    }

    public String description() {
        return description;
    }

    /**
     * Find the AruProduct by ARU product ID.
     * @param value product ID to search for
     * @return ARU product with matching
     */
    public static AruProduct fromProductId(String value) {
        for (AruProduct product : values()) {
            if (product.productId().equals(value)) {
                return product;
            }
        }
        throw new IllegalArgumentException("Invalid ARU data found in patch product ID: " + value);
    }

    public static boolean isKnownAruProduct(String value) {
        return Arrays.stream(values()).anyMatch(p -> p.toString().equals(value));
    }
}
