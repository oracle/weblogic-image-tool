// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        if (type == null || (version == null && commonName == null) || architecture == null) {
            return CommandResponse.success("IMG-0124");
        }
        String searchKey = commonName != null ? commonName : version;
        for (Map.Entry<String, Map<String, List<InstallerMetaData>>> entry : data.entrySet()) {
            String itemType = entry.getKey();

            if (type != null && type != InstallerType.fromString(itemType)) {
                continue;
            }
            Map<String, List<InstallerMetaData>> items = data.get(itemType);
            List<InstallerMetaData> installers = items.get(searchKey);
            if (installers == null || installers.isEmpty()) {
                return CommandResponse.error("IMG-0125");
            }

            boolean removed = false;
            if (commonName != null) {
                removed = installers.removeIf(installer ->
                    Architecture.fromString(installer.getArchitecture()).equals(architecture));

            } else {
                removed = installers.removeIf(installer -> installer.getProductVersion()
                    .equalsIgnoreCase(version)
                    && Architecture.fromString(installer.getArchitecture()).equals(architecture));
            }

            if (removed) {

                if (items.get(searchKey).isEmpty()) {
                    items.remove(searchKey);
                }

                try {
                    configManager.saveAllInstallers(data);
                    return CommandResponse.success("IMG-0169", searchKey, version, architecture);
                } catch (IOException e) {
                    throw new CacheStoreException(e.getMessage(), e);
                }

            }
        }
        return CommandResponse.error("IMG-0125");
    }

    @Option(
        names = {"-t", "--type"},
        description = "Filter installer type.  e.g. wls, jdk, wdt."
    )
    private InstallerType type;

    @Option(
        names = {"-cn", "--commonName"},
        description = "Filter by common name."
    )
    private String commonName;

    @Option(
        names = {"-v", "--version"},
        description = "Specific version to delete."
    )
    private String version;

    @Option(
        names = {"-a", "--architecture"},
        description = "Specific architecture to delete."
    )
    private Architecture architecture;



}
