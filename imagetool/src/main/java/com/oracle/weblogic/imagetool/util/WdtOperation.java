// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

/**
 * Command types for WebLogic Deploy Tooling.
 * See GitHub project - https://github.com/oracle/weblogic-deploy-tooling
 */
public enum WdtOperation {

    CREATE("createDomain.sh"),
    UPDATE("updateDomain.sh"),
    DEPLOY("deployApps.sh");

    WdtOperation(String script) {
        this.script = script;
    }

    private String script;

    /**
     * Get the WDT script to run that maps to this WDT operation.
     * @return the Unix shell script name for the WDT command.
     */
    public String getScript() {
        return script;
    }

}
