// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        sortOptions = false
)
public class AddPatchEntry extends CacheAddOperation {

    @Override
    public CommandResponse call() throws Exception {

        if (patchId != null && !patchId.isEmpty()) {
            List<String> patches = new ArrayList<>();
            patches.add(patchId);
            if (!Utils.validatePatchIds(patches, true)) {
                return new CommandResponse(1, "Patch ID validation failed");
            }
            return addToCache(patchId);
        } else {
            return new CommandResponse(1, "IMG-0076", "--patchId");
        }
    }

    @Option(
            names = {"--patchId"},
            description = "Patch number. Ex: 28186730",
            required = true
    )
    private String patchId;
}
