package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.api.model.InstallerType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver.CACHE_KEY_SEPARATOR;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.DEFAULT_WLS_VERSION;

@Command(
        name = "addInstaller",
        description = "Add cache entry for wls|fmw or jdk installer",
        //mixinStandardHelpOptions = false,
        sortOptions = false
)
public class AddInstallerEntry implements Callable<CommandResponse> {
    @Override
    public CommandResponse call() throws Exception {
        if (location != null && Files.isRegularFile(location)) {
            String key = String.format("%s%s%s", type, CACHE_KEY_SEPARATOR, version);
            META_RESOLVER.addToCache(key, location.toAbsolutePath().toString());
            return new CommandResponse(0, String.format("Successfully added to cache. %s=%s", key,
                    META_RESOLVER.getValueFromCache(key)));
        }
        return new CommandResponse(-1, "Command Failed. Check arguments. --path should exist on disk");
    }

    @Option(
            names = { "--type" },
            description = "Type of installer. Valid values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private InstallerType type;

    @Option(
            names = { "--ver" },
            description = "Installer version. Ex: For WLS|FMW use 12.2.1.3.0 For jdk, use 8u201",
            required = true,
            defaultValue = DEFAULT_WLS_VERSION
    )
    private String version;

    @Option(
            names = { "--path" },
            description = "Location on disk. For ex: /path/to/FMW/installer.zip",
            required = true
    )
    private Path location;

    @Unmatched
    List<String> unmatcheOptions;
}
