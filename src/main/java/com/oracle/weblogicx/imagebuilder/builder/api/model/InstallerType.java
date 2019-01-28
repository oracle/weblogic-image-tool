package com.oracle.weblogicx.imagebuilder.builder.api.model;

@SuppressWarnings("unused")
public enum InstallerType {
    FMW("fmw"),
    JDK("jdk"),
    WLS("wls");

    private String value;

    InstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
