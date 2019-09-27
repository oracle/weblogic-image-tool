// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DomainTypeTest {

    @Test
    public void fromValue() {
        assertEquals("validate domain type lowercase wls", DomainType.WLS, DomainType.fromValue("wls"));
        assertEquals("validate domain type uppercase WLS", DomainType.WLS, DomainType.fromValue("WLS"));

        assertEquals("validate domain type lowercase jrf", DomainType.JRF, DomainType.fromValue("jrf"));
        assertEquals("validate domain type uppercase JRF", DomainType.JRF, DomainType.fromValue("JRF"));

        assertEquals("validate domain type lowercase rjrf", DomainType.RestrictedJRF, DomainType.fromValue("rjrf"));
        assertEquals("validate domain type uppercase RJRF", DomainType.RestrictedJRF, DomainType.fromValue("RJRF"));

        assertNotEquals("string JRF does not equal WLS type", DomainType.WLS, DomainType.fromValue("jrf"));
    }
}