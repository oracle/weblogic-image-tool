// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

/**
 * Supported domain type. wls, jrf or restricted jrf.
 */
public enum DomainType {
    WLS("wls"),
    JRF("jrf"),
    RestrictedJRF("rjrf");

    private String value;

    DomainType(String value) {
        this.value = value;
    }

    /**
     * Create the Domain type Enum from the String value.
     * @param value the domain type string, ignoring case.
     * @return the enum domain type.
     */
    public static DomainType fromValue(String value) {
        for (DomainType eachType : DomainType.values()) {
            if (eachType.value.equalsIgnoreCase(value)) {
                return eachType;
            }
        }
        throw new IllegalArgumentException("argument " + value + " does not match any DomainType");
    }
}

