package com.oracle.weblogic.imagetool.api.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DomainTypeTest {

    @org.junit.Test
    public void fromValue() {
        assertEquals("validate domain type lowercase jrf", DomainType.JRF, DomainType.fromValue("jrf"));
        assertEquals("validate domain type uppercase jrf", DomainType.JRF, DomainType.fromValue("JRF"));
        assertEquals("validate domain type lowercase wls", DomainType.WLS, DomainType.fromValue("wls"));
        assertEquals("validate domain type uppercase WLS", DomainType.WLS, DomainType.fromValue("WLS"));
    }
}