package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.util.List;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.CLI_OPTION;

@Command(
        name = "getCacheDir",
        description = "Prints the cache directory path"
)
public class GetCacheDir implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        String path = META_RESOLVER.getCacheDir();
        if (unmatcheOptions.contains(CLI_OPTION)) {
            System.out.println("Cache Dir: " + path);
        }
        return new CommandResponse(0, "Cache Dir location: ", path);
    }

    @Unmatched
    List<String> unmatcheOptions;
}
