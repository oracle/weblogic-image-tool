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
import com.oracle.weblogic.imagetool.util.Architecture;
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
        Map<String, Map<String, List<InstallerMetaData>>> data = configManager.getInstallers();
        if (type == null || version == null || architecture == null) {
            return CommandResponse.success("IMG-0124");
        }
        boolean exists = false;
        for (String itemType : data.keySet()) {
            if (type != null && type != InstallerType.fromString(itemType)) {
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
                .map(list -> list.stream().anyMatch(i -> Architecture.fromString(i.getArchitecture())
                    .equals(architecture) && i.getProductVersion().equalsIgnoreCase(version)))
                .orElse(false);
            if (exists) {
                Optional.ofNullable(items.get(search))
                        .map(list -> list.removeIf(i -> Architecture.fromString(i.getArchitecture())
                            .equals(architecture) && i.getProductVersion().equalsIgnoreCase(version)));

                if (items.get(search).isEmpty()) {
                    items.remove(search);
                }
                break;
            }
        }
        if (!exists) {
            return CommandResponse.success("IMG-0125");
        }
        try {
            configManager.saveAllInstallers(data);
        } catch (IOException e) {
            throw new CacheStoreException(e.getMessage(), e);
        }
        return CommandResponse.success(null);
    }

    @Option(
        names = {"--type"},
        description = "Filter installer type.  e.g. wls, jdk, wdt."
    )
    private InstallerType type;

    @Option(
        names = {"--cn"},
        description = "Filter by common name."
    )
    private String commonName;

    @Option(
        names = {"--version"},
        description = "Specific version to delete."
    )
    private String version;

    @Option(
        names = {"--architecture"},
        description = "Specific architecture to delete."
    )
    private Architecture architecture;

}
