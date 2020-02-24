// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class UtilsTest {

    @Test
    void compareVersions() {
        assertEquals(0, Utils.compareVersions("12.2.1.3.0", "12.2.1.3.0"));
        assertTrue(Utils.compareVersions("1.0", "1.1") < 0);
        assertTrue(Utils.compareVersions("1.1", "1.0") > 0);
    }

    @Test
    void isEmptyString() {
        assertTrue(Utils.isEmptyString(""));
    }
}