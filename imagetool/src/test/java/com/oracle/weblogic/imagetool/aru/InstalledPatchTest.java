// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("unit")
class InstalledPatchTest {
    @Test
    void getPatchesTest() {
        // Parse the output from the image probe to get a list of installed patches
        String probeData = "32772437;24178014;\"One-off\";"
            + "32698246;24165861;\"WLS PATCH SET UPDATE 12.2.1.4.210330\";"
            + "122148;24146474;\"Bundle patch for Oracle Coherence Version 12.2.1.4.8\";";

        List<InstalledPatch> result = InstalledPatch.getPatchList(probeData);
        assertEquals(3, result.size(), "Parsing probe data failed to return correct number of bugs");
        List<String> expectedBugNumbers = Arrays.asList("32772437","32698246","122148");
        for (InstalledPatch patch : result) {
            if (!expectedBugNumbers.contains(patch.getBugNumber())) {
                //Array has 3, as expected, but one (or more) of those three is not the right bug number
                fail("List contained unexpected bug number: " + patch.getBugNumber());
            }
        }
    }

    @Test
    void getPsuVersionTest() {
        String probeData = "1234567;11111111;\"WLS PATCH SET UPDATE 12.2.1.4.191220\";";
        List<InstalledPatch> installedPatches = InstalledPatch.getPatchList(probeData);
        assertEquals("12.2.1.4.191220", InstalledPatch.getPsuVersion(installedPatches),
            "InstalledPatch.getPsuVersion is failing");
    }

    @Test
    void getPsuVersionWithIdTest() {
        String probeData = "1234567;11111111;\"WLS PATCH SET UPDATE 12.2.1.3.0(ID:200227.1409)\";";
        List<InstalledPatch> installedPatches = InstalledPatch.getPatchList(probeData);
        assertEquals("12.2.1.3.200227", InstalledPatch.getPsuVersion(installedPatches),
            "InstalledPatch.getPsuVersion is failing");
    }
}
