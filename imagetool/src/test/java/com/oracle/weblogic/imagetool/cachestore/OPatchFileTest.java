// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class OPatchFileTest {
    @Test
    void opatchPatchIdTest() {
        assertTrue(OPatchFile.isOPatchPatch("28186730"), "OPatch bug number");
        assertTrue(OPatchFile.isOPatchPatch("28186730_13.9.4.2.4"), "OPatch bug number with version");
    }
}
