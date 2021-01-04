// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class OPatchFileTest {
    @Test
    void opatchPatchIdTest() {
        assertTrue(OPatchFile.isOPatchPatch("28186730"), "OPatch bug number");
        assertTrue(OPatchFile.isOPatchPatch("28186730_13.9.4.2.4"), "OPatch bug number with version");
        assertTrue(OPatchFile.isOPatchPatch("28186730_1"), "OPatch bug number with separator");

        assertFalse(OPatchFile.isOPatchPatch("28186731"), "Should not match");
        assertFalse(OPatchFile.isOPatchPatch("281867301"), "OPatch bug number with additional number");
    }
}
