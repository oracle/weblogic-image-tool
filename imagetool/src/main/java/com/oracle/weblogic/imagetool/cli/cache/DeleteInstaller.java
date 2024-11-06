// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(
        name = "deleteInstaller",
        description = "Delete a installer"
)
public class DeleteInstaller extends CacheOperation {

    @Override
    public CommandResponse call() throws CacheStoreException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<InstallerType, Map<String, List<InstallerMetaData>>> data = configManager.getInstallers();
        if (type == null || version == null || architecture == null) {
            return CommandResponse.success("IMG-0124");
        }
        boolean exists = false;
        for (InstallerType itemType : data.keySet()) {
            if (type != null && type != itemType) {
                continue;
            }
            Map<String, List<InstallerMetaData>> items = data.get(itemType);
            String search;
            if (commonName == null) {
                search = version;
            } else {
                search = commonName;
            }
            exists = Optional.ofNullable(items.get(search))
                .map(list -> list.stream().anyMatch(i -> i.getPlatform().equalsIgnoreCase(architecture)
                    && i.getProductVersion().equalsIgnoreCase(version)))
                .orElse(false);
            if (exists) {
                Optional.ofNullable(items.get(search))
                        .map(list -> list.removeIf(i -> i.getPlatform().equalsIgnoreCase(architecture)
                            && i.getProductVersion().equalsIgnoreCase(version)));
                break;
            }
        }
        if (!exists) {
            return CommandResponse.success("IMG-0125");
        }
        try {
            configManager.saveAllInstallers(data, ConfigManager.getInstance().getInstallerDetailsFile());
        } catch (IOException e) {
            throw new CacheStoreException(e.getMessage(), e);
        }
        return CommandResponse.success(null);
    }

    @Option(
        names = {"--type"},
        description = "Filter installer type.  e.g. wls, jdk, wdt"
    )
    private InstallerType type;

    @Option(
        names = {"--cn"},
        description = "Filter installer type.  e.g. wls, jdk, wdt"
    )
    private String commonName;

    @Option(
        names = {"--version"},
        description = "Specific version to delete"
    )
    private String version;

    @Option(
        names = {"--architecture"},
        description = "Specific architecture to delete"
    )
    private String architecture;

}
