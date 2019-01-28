package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;

@Command(
        name = "deleteEntry",
        description = "Command to delete a cache entry"
//        mixinStandardHelpOptions = true
)
public class DeleteEntry implements Callable<CommandResponse> {

    @Option(
            names = { "--key" },
            description = "Key corresponding to the cache entry to delete",
            required = true
    )
    private String key;

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;

    @Override
    public CommandResponse call() throws Exception {
        if (key != null && !key.isEmpty()) {
            String oldValue = META_RESOLVER.deleteFromCache(key);
            return new CommandResponse(0, String.format("Deleted entry %s=%s", key, oldValue),
                    META_RESOLVER.deleteFromCache(key));
        }
        if (cliMode) {
            CommandLine.usage(this, System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key should correspond to a valid entry in cache");
    }
}
