// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
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
    public CommandResponse call() throws CacheStoreException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<InstallerType, Map<String, List<InstallerMetaData>>> data = configManager.getInstallers();

        for (InstallerType itemType : data.keySet()) {
            if (type != null && type != itemType) {
                continue;
            }
            System.out.println(itemType + ":");
            data.get(itemType).forEach((installer, metaData) -> {
                System.out.println("  " + installer + ":");
                for (InstallerMetaData meta : metaData) {
                    System.out.println("  - location: " + meta.getLocation());
                    System.out.println("    platform: " + meta.getPlatform());
                    System.out.println("    digest: " + meta.getDigest());
                    System.out.println("    dateAdded: " + meta.getDateAdded());
                    System.out.println("    version: " + meta.getProductVersion());
                }
            });
        }

        return CommandResponse.success(null);
    }

    @Option(
        names = {"--type"},
        description = "Filter installer type.  e.g. wls, jdk, wdt"
    )
    private InstallerType type;

}
