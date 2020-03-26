// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.wdt;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
public class WdtUnitTest {
    @Test
    void parseStringValues() {
        assertEquals(DomainType.JRF, DomainType.fromValue("jrf"));
        assertEquals(DomainType.JRF, DomainType.fromValue("JRF"));
        assertEquals(DomainType.JRF, DomainType.fromValue("Jrf"));

        assertEquals(DomainType.RestrictedJRF, DomainType.fromValue("rjrf"));
        assertEquals(DomainType.RestrictedJRF, DomainType.fromValue("RJRF"));
        assertEquals(DomainType.RestrictedJRF, DomainType.fromValue("RJrf"));

        assertEquals(DomainType.WLS, DomainType.fromValue("wls"));
        assertEquals(DomainType.WLS, DomainType.fromValue("WLS"));
        assertEquals(DomainType.WLS, DomainType.fromValue("Wls"));

        assertThrows(IllegalArgumentException.class, () -> DomainType.fromValue("RestrictedJRF"));
        assertThrows(IllegalArgumentException.class, () -> DomainType.fromValue("WCC"));
    }

    @Test
    void getInstallerType() {
        assertEquals(FmwInstallerType.WLS, DomainType.WLS.installerType());
        assertEquals(FmwInstallerType.FMW, DomainType.JRF.installerType());
    }

    @Test
    void wdtOperations() {
        assertEquals("createDomain.sh", WdtOperation.CREATE.getScript());
        assertEquals("updateDomain.sh", WdtOperation.UPDATE.getScript());
    }
}
