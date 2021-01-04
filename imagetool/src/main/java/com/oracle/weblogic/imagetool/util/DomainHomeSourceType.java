// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

public enum DomainHomeSourceType {
    MODEL("FromModel"),
    IMAGE("Image"),
    PV("PersistentVolume");

    private String value;

    /**
     * Create an Enum value for the domain home source type.
     * @param value value the value to use for substitution in the mustache template.
     */
    DomainHomeSourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
