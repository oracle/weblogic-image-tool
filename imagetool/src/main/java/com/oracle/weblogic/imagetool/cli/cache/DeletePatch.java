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
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> data = configManager.getAllPatches();

        if (patchId == null || version == null || architecture == null) {
            return CommandResponse.success("IMG-0126");
        }
        for (String id : data.keySet()) {
            if (patchId.equalsIgnoreCase(id)) {
                List<PatchMetaData> items = data.get(id);
                if (isPatchVersionMatched(items)) {

                    Optional<PatchMetaData> d = Optional.of(items).flatMap(list -> list.stream().filter(i ->
                        i.getPatchVersion().equals(version)
                            && Architecture.fromString(i.getArchitecture()).equals(architecture)).findAny());

                    if (d.isPresent()) {
                        Optional.of(items)
                            .map(list -> list.removeIf(i -> i.getPatchVersion().equals(version)
                                && Architecture.fromString(i.getArchitecture()).equals(architecture)));
                        // if all patches are removed for this bug number, remove this bug number from the store.
                        if (items.isEmpty()) {
                            data.remove(id);
                        }
                        try {
                            configManager.saveAllPatches(data);
                        } catch (IOException e) {
                            throw new CacheStoreException(e.getMessage(), e);
                        }

                        return CommandResponse.success("IMG-0168", patchId, version, architecture);
                    }


                } else {
                    return CommandResponse.error("IMG-0127");
                }

            }
        }
        return CommandResponse.error("IMG-0127");
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
