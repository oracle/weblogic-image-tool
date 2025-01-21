// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
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
    public CommandResponse call() {

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

            Map<String, List<PatchMetaData>> groupByArchitecture = data.get(bug).stream()
                .collect(Collectors.groupingBy(PatchMetaData::getArchitecture));

            groupByArchitecture.forEach((architecture, metaDatas) -> {
                System.out.println("  " + architecture + ":");

                metaDatas.forEach(metaData -> {
                    if (version != null && !version.isEmpty()) {
                        if (!version.equalsIgnoreCase(metaData.getPatchVersion())) {
                            return;
                        }
                    }
                    System.out.println("     - version: " + metaData.getPatchVersion());
                    System.out.println("       location: " + metaData.getLocation());
                    if (details) {
                        System.out.println("        digest: " + metaData.getDigest());
                        System.out.println("        dateAdded: " + metaData.getDateAdded());
                    }

                });

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
        description = "List only the patch version"
    )
    private String version;

    @Option(
        names = {"--details"},
        description = "List all details about the patch"
    )
    private boolean details = false;
}
