// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.util.Utils;

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
     * The fields should be separated by a semi-colon with three fields per patch.
     * @param patchesString patch data from the inspected image
     * @return a list of patch objects
     */
    public static List<InventoryPatch> parseInventoryPatches(String patchesString) {
        List<InventoryPatch> patches = new ArrayList<>();
        if (!Utils.isEmptyString(patchesString)) {
            String[] tokens = patchesString.split(";");
            for (int i = 0; i < tokens.length; i++) {
                InventoryPatch patch = new InventoryPatch();
                patch.bug = tokens[i];
                if (i++ < tokens.length) {
                    patch.uid = tokens[i];
                }
                if (i++ < tokens.length) {
                    patch.description = tokens[i].replace("\"", "");
                }
                patches.add(patch);
            }
        }
        return patches;
    }
}
