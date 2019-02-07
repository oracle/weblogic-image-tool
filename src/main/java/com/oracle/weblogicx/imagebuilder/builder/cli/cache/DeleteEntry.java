package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

import java.util.List;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.CACHE_DIR_KEY;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.CLI_OPTION;

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

    @Spec
    CommandSpec spec;

    @Unmatched
    List<String> unmatcheOptions;

    @Override
    public CommandResponse call() throws Exception {
        if (key != null && !key.isEmpty()) {
            if (CACHE_DIR_KEY.equals(key.toLowerCase())) {
                return new CommandResponse(-1, "Error: Cannot delete cache.dir entry. Use setCacheDir instead");
            }
            String oldValue = META_RESOLVER.deleteFromCache(key);
            if (oldValue != null) {
                return new CommandResponse(0, String.format("Deleted entry %s=%s", key, oldValue),
                        oldValue);
            } else {
                return new CommandResponse(0, "Nothing to delete for key: " + key);
            }
        }
        if (unmatcheOptions.contains(CLI_OPTION)) {
            spec.commandLine().usage(System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key should correspond to a valid entry in cache");
    }
}
