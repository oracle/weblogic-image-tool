package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;

@Command(
        name = "addEntry",
        description = "Command to add a cache entry. Use caution"
//        mixinStandardHelpOptions = true
)
public class AddEntry implements Callable<CommandResponse> {

    @Option(
            names = {"--key"},
            description = "Key for the cache entry",
            required = true
    )
    private String key;

    @Option(
            names = {"--path"},
            description = "Value for the cache entry. A file path on the local disk",
            required = true
    )
    private Path location;

    @Option(
            names = {"--cli"},
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;

    @Override
    public CommandResponse call() throws Exception {
        if (key != null && !key.isEmpty() && location != null && Files.exists(location)) {
            String oldValue = META_RESOLVER.getValueFromCache(key);
            String msg;
            if (oldValue != null) {
                msg = String.format("Replaced old value %s with new value %s for key %s", oldValue, location, key);
            } else {
                msg = String.format("Added entry %s=%s", key, location);
            }
            if (META_RESOLVER.addToCache(key, location.toAbsolutePath().toString())) {
                return new CommandResponse(0, msg);
            } else {
                return new CommandResponse(-1, "Command Failed");
            }
        }
        if (cliMode) {
            CommandLine.usage(this, System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key & --path required.");
    }
}
