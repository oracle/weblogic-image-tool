// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

public enum KubernetesTarget {
    DEFAULT("Default"),
    OPENSHIFT("OpenShift");

    private String value;

    /**
     * Create an Enum value for the installer type.
     * @param value value is the first part of the key for the installer in the cache, and the filename of the
     *              response file
     */
    KubernetesTarget(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
