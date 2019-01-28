package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;

@Command(
        name = "listItems",
        description = "List cache contents",
        mixinStandardHelpOptions = true
)
public class ListCacheItems implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        Map<String, String> resultMap = META_RESOLVER.getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            if (cliMode) {
                System.out.println("cache is empty");
            }
            return new CommandResponse(0, "cache is empty", Collections.<String, String> emptyMap());
        } else {
            if (cliMode) {
                System.out.println("Cache contents");
                resultMap.forEach((key, value) -> {
                    System.out.println(key + "=" + value);
                });
            }
            return new CommandResponse(0, "Cache contents", resultMap);
        }
    }

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;

}
