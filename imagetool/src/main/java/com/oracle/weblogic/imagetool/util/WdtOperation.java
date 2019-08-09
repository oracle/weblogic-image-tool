// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

public enum WdtOperation {

    CREATE("createDomain.sh"),
    UPDATE("updateDomain.sh");

    WdtOperation(String script) {
        this.script = script;
    }

    private String script;

    public String getScript() {
        return script;
    }

}
