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

    @Override
    public CommandResponse call() throws CacheStoreException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> data = configManager.getAllPatches();
        if (patchId == null || version == null || architecture == null) {
            return CommandResponse.success("IMG-0126");
        }
        boolean exists = false;
        for (String id : data.keySet()) {
            if (patchId != null && !patchId.equalsIgnoreCase(id)) {
                continue;
            }
            List<PatchMetaData> items = data.get(id);
            exists = Optional.ofNullable(items)
                .map(list -> list.stream().anyMatch(i -> i.getPatchVersion().equals(version)
                    && Architecture.fromString(i.getPlatform()).equals(architecture))).orElse(false);
            if (exists) {
                Optional.ofNullable(items)
                        .map(list -> list.removeIf(i -> i.getPatchVersion().equals(version)
                            && Architecture.fromString(i.getPlatform()).equals(architecture)));
                
                if (items.isEmpty()) {
                    data.remove(id);
                }

                break;
            }
        }
        if (!exists) {
            return CommandResponse.success("IMG-0127");
        }
        try {
            configManager.saveAllPatches(data, ConfigManager.getInstance().getPatchDetailsFile());
        } catch (IOException e) {
            throw new CacheStoreException(e.getMessage(), e);
        }
        return CommandResponse.success(null);
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
