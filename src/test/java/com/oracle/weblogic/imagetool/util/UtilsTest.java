/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
*/
package com.oracle.weblogic.imagetool.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Test
    public void compareVersions() {
        assertEquals(0, Utils.compareVersions("12.2.1.3.0", "12.2.1.3.0"));
        assertTrue(Utils.compareVersions("1.0", "1.1") < 0);
        assertTrue(Utils.compareVersions("1.1", "1.0") > 0);
    }

    @Test
    public void isEmptyString() {
        assertTrue(Utils.isEmptyString(""));
    }
}