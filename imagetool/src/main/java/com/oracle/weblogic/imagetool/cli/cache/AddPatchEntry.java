// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Collections;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        sortOptions = false
)
public class AddPatchEntry extends CacheAddOperation {

    public String getKey() {
        return patchId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCommonName() {
        return null;
    }

    @Override
    public Architecture getArchitecture() {
        return architecture;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getBaseFMWVersion() {
        return "";
    }

    @Override
    public CommandResponse call() throws Exception {
        try {
            if (patchId != null && !patchId.isEmpty()) {
                Utils.validatePatchIds(Collections.singletonList(patchId), true);
                return addPatchToCache();
            } else {
                return CommandResponse.error("IMG-0076", "--patchId");
            }
        } catch (Exception ex) {
            return CommandResponse.error(ex.getMessage());
        }
    }

    @Option(
            names = {"--patchId"},
            description = "Patch number. Ex: 28186730",
            required = true
    )
    private String patchId;

    @Option(
        names = {"-v", "--version"},
        description = "Patch version. ",
        required = true
    )
    private String version;

    @Option(
        names = {"-a", "--architecture"},
        required = true,
        description = "Patch architecture. Valid values: arm64, amd64, Generic"
    )
    private Architecture architecture;

    @Option(
        names = {"-d", "--description"},
        description = "Patch description."
    )
    private String description;
}
