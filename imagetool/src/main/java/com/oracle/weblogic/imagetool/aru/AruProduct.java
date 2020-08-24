// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

/**
 * ARU product codes for patching.
 */
public enum AruProduct {
    WLS("15991", "Oracle WebLogic Server"),
    COH("13964", "Oracle Coherence"),
    OSB("16011", "Oracle Service Bus"),
    SOA("12745", "Oracle SOA Suite"),
    IDM("18391", "Oracle Identity Manager"),
    OAM("18388", "Oracle Access Manager"),
    OUD("19748", "Oracle Unified Directory"),
    WCC("13946", "Oracle WebCenter Content"),
    WCP("15224", "Oracle WebCenter Portal"),
    WCS("20995", "Oracle WebCenter Sites"),
    JDEV("11281", "Oracle JDeveloper")
    //FIT("33256", "Fusion Internal Tools") No longer used after July 2020 PSU
    ;

    private String productId;
    private String description;
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
}
