// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(
        name = "listInstallers",
        description = "List installers"
)
public class ListInstallers extends CacheOperation {

    @Override
    public CommandResponse call() {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, Map<String, List<InstallerMetaData>>> data = configManager.getInstallers();
        verifyInput();

        for (String itemType : data.keySet()) {
            if (type != null && type != InstallerType.fromString(itemType)) {
                continue;
            }
            System.out.println(itemType + ":");
            data.get(itemType).forEach((installer, metaData) -> {
                if (commonName != null && !commonName.equalsIgnoreCase(installer)) {
                    return;
                }
                // commonName is null or version is specified
                if (version != null && !version.equalsIgnoreCase(installer)) {
                    return;
                }
                System.out.println("  " + installer + ":");

                List<InstallerMetaData> sortedList = metaData.stream()
                    .sorted(Comparator.comparing(InstallerMetaData::getArchitecture))
                    .collect(Collectors.toList());

                String currentArch = "";
                for (InstallerMetaData meta : sortedList) {
                    if (!currentArch.equals(meta.getArchitecture())) {
                        currentArch = meta.getArchitecture();
                        System.out.println("    " + meta.getArchitecture() + ":");
                    }
                    System.out.println("      version: " + meta.getProductVersion());
                    System.out.println("      location: " + meta.getLocation());
                    if (details) {
                        System.out.println("      digest: " + meta.getDigest());
                        System.out.println("      dateAdded: " + meta.getDateAdded());
                    }
                }
            });
        }

        return CommandResponse.success(null);
    }

    private void verifyInput() {
        if (commonName != null && !commonName.isEmpty() && type == null) {
            System.out.println("--type cannot be null when commonName is specified");
            System.exit(2);
        }

        if (version != null && !version.isEmpty() && type == null) {
            System.out.println("--type cannot be null when version is specified");
            System.exit(2);
        }
    }

    @Option(
        names = {"--type"},
        description = "Filter installer type.  e.g. wls, jdk, wdt"
    )
    private InstallerType type;

    @Option(
        names = {"--commonName"},
        description = "Filter installer by common name."
    )
    private String commonName;

    @Option(
        names = {"--version"},
        description = "Filter installer by version."
    )
    private String version;

    @Option(
        names = {"--details"},
        description = "Full details of the installers."
    )
    private boolean details = false;

}
