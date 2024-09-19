// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryPatch {
    private String bug;
    private String uid;
    private String description;

    public String bug() {
        return bug;
    }

    public String uid() {
        return uid;
    }

    public String description() {
        return description;
    }

    /**
     * Parse the provided string into patch objects, and return the list of patches.
     * The fields should be separated by a semicolon with three fields per patch.
     * @param patchesString patch data from the inspected image
     * @return a list of patch objects
     */
    public static List<InventoryPatch> parseInventoryPatches(String patchesString) {
        List<InventoryPatch> patches = new ArrayList<>();
        // Pattern defines a tuple of three elements: patch ID, patch UID, and patch description.
        Pattern tuplePattern = Pattern.compile("(\\d+);(\\d+);\\\"(.*?)\\\";?");
        Matcher patchMatcher = tuplePattern.matcher(patchesString);
        while (patchMatcher.find()) {
            InventoryPatch patch = new InventoryPatch();
            patch.bug = patchMatcher.group(1);
            patch.uid = patchMatcher.group(2);
            patch.description = patchMatcher.group(3);
            patches.add(patch);
        }

        return patches;
    }
}
