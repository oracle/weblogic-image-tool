// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Architecture;
import picocli.CommandLine.Option;


public abstract class CacheAddOperation extends CacheOperation {

    public abstract String getKey();

    public abstract String getVersion();

    public abstract String getCommonName();

    public abstract Architecture getArchitecture();

    public abstract String getDescription();

    public abstract String getBaseFMWVersion();

    CommandResponse addInstallerToCache() throws IOException {
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return CommandResponse.error("IMG-0049", filePath);
        }

        String type = getKey();
        String name = getCommonName();

        if (name == null) {
            name = getVersion();
        }

        Architecture arch = getArchitecture();
        InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.fromString(type),
            arch, name);
        if (metaData != null) {
            return CommandResponse.success("IMG-0075");
        }
        metaData = new InstallerMetaData(arch.toString(), filePath.toAbsolutePath().toString(),
            getVersion(), getBaseFMWVersion());
        ConfigManager.getInstance().addInstaller(InstallerType.fromString(type), getCommonName(), metaData);
        // if the new value is the same as the existing cache value, do nothing

        return CommandResponse.success("IMG-0050", type, metaData.getProductVersion(), metaData.getLocation());
    }

    CommandResponse addPatchToCache() throws IOException {
        // if file is invalid or does not exist, return an error
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return CommandResponse.error("IMG-0049", filePath);
        }

        String bugNumber = getKey();
        String version = getVersion();

        int separator = bugNumber.indexOf(CacheStore.CACHE_KEY_SEPARATOR);
        if (separator > 0) {
            version = bugNumber.substring(separator + 1);
            bugNumber = bugNumber.substring(0, separator);
        }

        PatchMetaData metaData = ConfigManager.getInstance().getPatchForPlatform(getArchitecture().toString(),
            bugNumber, version);

        if (metaData != null) {
            return CommandResponse.success("IMG-0075");
        }

        Architecture arch = getArchitecture();

        ConfigManager.getInstance().addPatch(bugNumber, arch.toString(), filePath.toAbsolutePath().toString(),
            version, getDescription());

        return CommandResponse.success("IMG-0130", bugNumber, version,
            filePath.toAbsolutePath().toString());
    }

    @Option(
        names = {"--force"},
        description = "Overwrite existing entry, if it exists"
    )
    private boolean force = false;


    @Option(
        names = {"-p", "--path"},
        description = "Location of the file on disk. For ex: /path/to/patch-or-installer.zip",
        required = true
    )
    private Path filePath;
}
