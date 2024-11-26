// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(
        name = "listPatches",
        description = "List patches"
)
public class ListPatches extends CacheOperation {

    @Override
    public CommandResponse call() throws CacheStoreException {

        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> data = configManager.getAllPatches();
        if (version != null && !version.isEmpty()) {
            if (patchId == null) {
                System.out.println("--patchId cannot be null when version is specified");
                System.exit(2);
            }
        }

        for (String bug : data.keySet()) {
            if (patchId != null && !patchId.equalsIgnoreCase(bug)) {
                continue;
            }
            System.out.println(bug + ":");
            data.get(bug).forEach((metaData) -> {
                if (version != null && !version.isEmpty()) {
                    if (!version.equalsIgnoreCase(metaData.getPatchVersion())) {
                        return;
                    }
                }
                System.out.println("  - location: " + metaData.getLocation());
                System.out.println("    platform: " + metaData.getPlatform());
                System.out.println("    digest: " + metaData.getHash());
                System.out.println("    dateAdded: " + metaData.getDateAdded());
                System.out.println("    version: " + metaData.getPatchVersion());
            });
        }

        return CommandResponse.success(null);

    }

    @Option(
        names = {"--patchId"},
        description = "Patch id"
    )
    private String patchId;

    @Option(
        names = {"--version"},
        description = "Patch version"
    )
    private String version;
}
