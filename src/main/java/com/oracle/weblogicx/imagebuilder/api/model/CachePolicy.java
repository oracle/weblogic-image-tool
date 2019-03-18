/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/

package com.oracle.weblogicx.imagebuilder.api.model;

/**
 * first - Use cache entries and download artifacts if required
 * always - Use only cache entries and never download artifacts
 * never - Ignore cache entries and always download artifacts
 */
public enum CachePolicy {
    FIRST("first"),
    ALWAYS("always"),
    NEVER("never");

    private String value;

    CachePolicy(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
