// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class VersionTest {
    @Test
    void sameVersionNumber() {
        Version a = new Version("1.2.3");
        Version b = new Version("1.2.3");
        assertEquals(0, b.compareTo(a));
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void differentVersionNumbers() {
        Version a = new Version("1.2.3");
        Version b = new Version("1.2.4");
        assertEquals(1, b.compareTo(a));
        assertEquals(-1, a.compareTo(b));
    }

    @Test
    void differentVersionLengths() {
        Version a = new Version("1.2.3");
        Version b = new Version("1.2.3.1");
        assertEquals(1, b.compareTo(a));
        assertEquals(-1, a.compareTo(b));
    }

    @Test
    void integerComparison() {
        Version a = new Version("13.9.4.2.9");
        Version b = new Version("13.9.4.2.10");
        assertEquals(1, b.compareTo(a));
        assertEquals(-1, a.compareTo(b));
    }

    @Test
    void nonNumericVersion() {
        assertThrows(NumberFormatException.class, () -> new Version("1.A.4"));
        assertThrows(NumberFormatException.class, () -> new Version("1.2.3-SNAP"));
    }

    @Test
    void allowsNull() {
        Version a = new Version(null);
        Version b = new Version("1.2.3");
        assertEquals(1, b.compareTo(a));
        assertEquals(-1, a.compareTo(b));
    }

    @Test
    void equalObjects() {
        Version a = new Version("1.2.3");
        Version b = new Version("1.2.3");
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a, a);
    }

    @Test
    void hashcodeTest() {
        Version a = new Version("1.2.3");
        Version b = new Version("1.2.3");
        Version c = new Version("1.2.4");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
    }
}
