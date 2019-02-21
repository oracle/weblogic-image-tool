/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;

import java.util.Collections;
import java.util.Map;

@Command(
        name = "listItems",
        description = "List cache contents"
)
public class ListCacheItems extends CacheOperation {

    @Override
    public CommandResponse call() throws Exception {
        Map<String, String> resultMap = cacheStore.getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
                System.out.println("cache is empty");
            }
            return new CommandResponse(0, "cache is empty", Collections.<String, String> emptyMap());
        } else {
            if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
                System.out.println("Cache contents");
                resultMap.forEach((key, value) -> {
                    System.out.println(key + "=" + value);
                });
            }
            return new CommandResponse(0, "Cache contents", resultMap);
        }
    }

//    @Option(
//            names = {"--cacheStoreType"},
//            description = "Whether to use file backed cache store or preferences backed cache store. Ex: file or pref",
//            hidden = true,
//            defaultValue = "file"
//    )
//    private CacheStore cacheStore;
//
//    @Unmatched
//    List<String> unmatchedOptions;
}
