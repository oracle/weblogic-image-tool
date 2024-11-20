// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.util.Architecture;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addInstaller",
        description = "Add cache entry for wls, fmw, jdk or wdt installer",
        sortOptions = false
)
public class AddInstallerEntry extends CacheAddOperation {

    @Override
    public CommandResponse call() throws IOException, CacheStoreException {
        if ("NONE".equalsIgnoreCase(version)) {
            throw new IllegalArgumentException("IMG-0105");
        }

        return addInstallerToCache();
    }

    @Override
    public String getKey() {
        return type.toString();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCommonName() {
        if (commonName == null) {
            return version;
        }
        return commonName;
    }

    @Override
    public Architecture getArchitecture() {
        return architecture;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Option(
            names = {"-t", "--type"},
            description = "Type of installer. Valid values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private InstallerType type;

    @Option(
            names = {"-v", "--version"},
            description = "Installer version. Ex: For WLS|FMW use 12.2.1.3.0 For jdk, use 8u201",
            required = true
    )
    private String version;

    @Option(
        names = {"-a", "--architecture"},
        required = true,
        description = "Installer architecture. Valid values: ${COMPLETION-CANDIDATES}"
    )
    private Architecture architecture;

    @Option(
        names = {"-c", "--commonName"},
        description = "(Optional) common name. Valid values:  Alphanumeric values with no special characters. "
            + "If not specified, default to the version value."
    )
    private String commonName;

}
