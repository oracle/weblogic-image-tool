package com.oracle.weblogicx.imagebuilder.builder.api.model;

import java.util.ArrayList;
import java.util.List;

import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.BUILD_ARG;

@SuppressWarnings("unused")
public enum InstallerType {
    
    FMW(WLSInstallerType.FMW.toString()),
    JDK("jdk"),
    WDT("wdt"),
    WLS(WLSInstallerType.WLS.toString());

    private String value;

    InstallerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public List<String> getBuildArg(String location) {
        List<String> retVal = new ArrayList<>(2);
        retVal.add(BUILD_ARG);
        if (this == WLS || this == FMW) {
            retVal.add("WLS_PKG=" + location);
        } else if (this == JDK) {
            retVal.add("JAVA_PKG=" + location);
        } else {
            retVal.add("WDT_PKG=" + location);
        }
        return retVal;
    }

    public static InstallerType fromValue(String value) {
        for (InstallerType eachType : InstallerType.values()) {
            if (eachType.value.equalsIgnoreCase(value)) {
                return eachType;
            }
        }
        throw new IllegalArgumentException("argument " + value + " does not match any InstallerType");
    }
}
