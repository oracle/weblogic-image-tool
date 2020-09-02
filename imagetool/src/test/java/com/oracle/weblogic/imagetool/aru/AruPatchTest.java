// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.util.HttpUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class AruPatchTest {

    @Test
    void testRecommendedPatches() throws Exception {
        List<AruPatch> aruPatches;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
            this.getClass().getResourceAsStream("/recommended-patches.xml")))) {

            String aruInfo = buffer.lines().collect(Collectors.joining("\n"));
            Document xml = HttpUtil.parseXmlString(aruInfo);

            aruPatches = AruPatch.getPatches(xml);
        }
        assertEquals(5, aruPatches.size());
    }

}