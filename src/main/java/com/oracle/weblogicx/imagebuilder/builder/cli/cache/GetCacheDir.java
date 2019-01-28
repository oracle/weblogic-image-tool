package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;

@Command(
        name = "getCacheDir",
        description = "Prints the cache directory path"
)
public class GetCacheDir implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        String path = META_RESOLVER.getCacheDir();
        if (cliMode) {
            System.out.println("Cache Dir: " + path);
        }
        return new CommandResponse(0, "Cache Dir location: ", path);
    }

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;
}
