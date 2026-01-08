// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Architecture;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(
        name = "deletePatch",
        description = "Delete a patch"
)
public class DeletePatch extends CacheOperation {

    private boolean isPatchVersionMatched(List<PatchMetaData> items) {
        return Optional.ofNullable(items)
            .map(list -> list.stream().anyMatch(i -> i.getPatchVersion().equals(version)
                && Architecture.fromString(i.getArchitecture()).equals(architecture))).orElse(false);
    }

    @Override
    public CommandResponse call() throws CacheStoreException {
        // Early validation - fail fast if required parameters are missing
        if (patchId == null || version == null || architecture == null) {
            return CommandResponse.success("IMG-0126");
        }

        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> data = configManager.getAllPatches();

        // Use direct map lookup instead of iterating through all keys
        List<PatchMetaData> patches = data.get(patchId);
        if (patches == null) {
            return CommandResponse.error("IMG-0127");
        }

        // Check version compatibility first
        if (!isPatchVersionMatched(patches)) {
            return CommandResponse.error("IMG-0127");
        }

        // Find and remove the matching patch in a single operation
        boolean removed = patches.removeIf(patch ->
            version.equals(patch.getPatchVersion())
                && architecture.equals(Architecture.fromString(patch.getArchitecture()))
        );

        if (!removed) {
            return CommandResponse.error("IMG-0127");
        }

        // Clean up empty patch list
        if (patches.isEmpty()) {
            data.remove(patchId);
        }

        // Save changes
        try {
            configManager.saveAllPatches(data);
            return CommandResponse.success("IMG-0168", patchId, version, architecture);
        } catch (IOException e) {
            throw new CacheStoreException(e.getMessage(), e);
        }
    }

    @Option(
        names = {"--patchId"},
        description = "Bug num"
    )
    private String patchId;

    @Option(
        names = {"--version"},
        description = "Specific version to delete"
    )
    private String version;

    @Option(
        names = {"--architecture"},
        description = "Specific architecture to delete"
    )
    private Architecture architecture;

}
