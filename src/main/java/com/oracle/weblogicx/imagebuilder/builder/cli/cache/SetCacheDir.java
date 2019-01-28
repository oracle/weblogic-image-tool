package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;

@Command(
        name = "setCacheDir",
        description = "Sets the cache directory where to download required artifacts"
)
public class SetCacheDir implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        String oldValue = META_RESOLVER.getCacheDir();
        String msg;
        if (oldValue != null) {
            msg = String.format("Changed cache dir from %s to %s", oldValue, location);
        } else {
            msg = String.format("Set cache dir to: %s", location);
        }

        if (META_RESOLVER.setCacheDir(location.toAbsolutePath().toString())) {
            if (cliMode) {
                System.out.println(msg);
            }
            return new CommandResponse(0, msg);
        } else {
            return new CommandResponse(-1, "setCacheDir failed");
        }
    }

    @Parameters(
            arity = "1",
            description = "A directory on local disk. Ex: /path/to/my/dir",
            index = "0"
    )
    private Path location;

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;
}
