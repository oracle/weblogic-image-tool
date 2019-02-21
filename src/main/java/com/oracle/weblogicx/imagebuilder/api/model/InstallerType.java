package com.oracle.weblogicx.imagebuilder.api.model;

import com.oracle.weblogicx.imagebuilder.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * An enum to represent type of installer
 */
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
        retVal.add(Constants.BUILD_ARG);
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
