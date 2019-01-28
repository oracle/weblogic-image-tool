/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.api.model;

/**
 * Supported installer type. WebLogic Server and FMW Infrastructure.
 */
@SuppressWarnings("unused")
public enum WLSInstallerType {
    WLS("wls"),
    FMW("fmw");

    private String value;

    WLSInstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
