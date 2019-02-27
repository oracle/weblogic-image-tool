/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.oracle.weblogicx.imagebuilder.api.meta.CacheStore.CACHE_KEY_SEPARATOR;

@Command(
        name = "addInstaller",
        description = "Add cache entry for wls, fmw, jdk or wdt installer",
        sortOptions = false
)
public class AddInstallerEntry extends CacheOperation {

    public AddInstallerEntry() {
    }

    public AddInstallerEntry(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {
        if (location != null && Files.isRegularFile(location)) {
            String key = String.format("%s%s%s", type, CACHE_KEY_SEPARATOR, version);
            cacheStore.addToCache(key, location.toAbsolutePath().toString());
            return new CommandResponse(0, String.format("Successfully added to cache. %s=%s", key,
                    cacheStore.getValueFromCache(key)));
        }
        return new CommandResponse(-1, "Command Failed. Check arguments. --path should exist on disk");
    }

    @Option(
            names = {"--type"},
            description = "Type of installer. Valid values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private InstallerType type;

    @Option(
            names = {"--version"},
            description = "Installer version. Ex: For WLS|FMW use 12.2.1.3.0 For jdk, use 8u201",
            required = true,
            defaultValue = Constants.DEFAULT_WLS_VERSION
    )
    private String version;

    @Option(
            names = {"--path"},
            description = "Location on disk. For ex: /path/to/FMW/installer.zip",
            required = true
    )
    private Path location;
}
