// Copyright (c) 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AruPatchTest {
    @Test
    void testIsStackPatchBundle() {
        assertTrue(new AruPatch().description("WebLogic Stack Patch Bundle 12.2.1.4").isStackPatchBundle());
        assertTrue(new AruPatch().description("weblogic stack patch bundle 14.1.2").isStackPatchBundle());
        assertFalse(new AruPatch().description("WEBLOGIC SERVER PATCH FOR BUG 38792523").isStackPatchBundle());
        assertFalse(new AruPatch().isStackPatchBundle());
    }
}
