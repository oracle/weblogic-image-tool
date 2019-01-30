/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.api.model;

@SuppressWarnings("unused")
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
