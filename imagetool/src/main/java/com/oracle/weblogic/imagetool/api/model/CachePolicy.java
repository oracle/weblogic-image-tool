// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

/**
 * Cache policy types.
 * @Deprecated
 *   This is subject to removal and there is no need to use it
 *   The logic is changed to use the presence of userId and password to
 *   determine whether to access online if the item is not in the cache.
 *   It allows the cache is just a store and nothing more
 *
 * always - Use only cache entries and never download artifacts
 */
public enum CachePolicy {
    ALWAYS("always");

    private String value;

    CachePolicy(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
