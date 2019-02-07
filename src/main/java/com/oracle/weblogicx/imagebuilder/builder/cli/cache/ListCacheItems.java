package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Unmatched;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.CLI_OPTION;

@Command(
        name = "listItems",
        description = "List cache contents"
        //mixinStandardHelpOptions = true
)
public class ListCacheItems implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        Map<String, String> resultMap = META_RESOLVER.getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            if (unmatcheOptions.contains(CLI_OPTION)) {
                System.out.println("cache is empty");
            }
            return new CommandResponse(0, "cache is empty", Collections.<String, String> emptyMap());
        } else {
            if (unmatcheOptions.contains(CLI_OPTION)) {
                System.out.println("Cache contents");
                resultMap.forEach((key, value) -> {
                    System.out.println(key + "=" + value);
                });
            }
            return new CommandResponse(0, "Cache contents", resultMap);
        }
    }

    @Unmatched
    List<String> unmatcheOptions;
}
