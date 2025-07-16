// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Utils;
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
            printLine(itemType + ":");

            data.get(itemType).forEach((installer, metaData) -> {

                if (commonName != null && !commonName.equalsIgnoreCase(installer)) {
                    return;
                }
                // commonName is null or version is specified
                if (commonName == null && version != null && !version.equalsIgnoreCase(installer)) {
                    return;
                }
                printLine("  " + installer + ":");
                List<InstallerMetaData> sortedList = new ArrayList<>(metaData);

                if (commonName != null && version != null) {
                    sortedList = metaData.stream()
                        .filter(c -> c.getProductVersion().equals(version))
                        .sorted(Comparator.comparing(InstallerMetaData::getArchitecture))
                        .collect(Collectors.toList());

                } else {
                    sortedList = metaData.stream()
                        .sorted(Comparator.comparing(InstallerMetaData::getArchitecture))
                        .collect(Collectors.toList());
                }

                printDetails(sortedList, InstallerType.fromString(itemType));
            });

        }

        return CommandResponse.success(null);
    }

    private void printDetails(List<InstallerMetaData> sortedList, InstallerType type) {
        String currentArch = "";
        for (InstallerMetaData meta : sortedList) {
            if (!currentArch.equals(meta.getArchitecture())) {
                currentArch = meta.getArchitecture();
                printLine("    " + meta.getArchitecture() + ":");
            }
            printLine("      version: " + meta.getProductVersion());
            printLine("      location: " + meta.getLocation());
            if (details) {
                printLine("      digest: " + meta.getDigest());
                printLine("      dateAdded: " + meta.getDateAdded());
                if (!Utils.isBaseInstallerType(type)) {
                    printLine("      baseFMWVersion: " + meta.getBaseFMWVersion());
                }
            }
        }
    }

    private void printLine(String line) {
        System.out.println(line);
    }

    private void verifyInput() {
        if (commonName != null && !commonName.isEmpty() && type == null) {
            printLine(Utils.getMessage("IMG-0156"));
            System.exit(2);
        }

        if (version != null && !version.isEmpty() && type == null) {
            printLine(Utils.getMessage("IMG-0157"));
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
