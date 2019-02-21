/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;

@Command(
        name = "getCacheDir",
        description = "Prints the cache directory path"
)
public class GetCacheDir extends CacheOperation {

    @Override
    public CommandResponse call() throws Exception {
        String path = cacheStore.getCacheDir();
        if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
            System.out.println("Cache Dir: " + path);
        }
        return new CommandResponse(0, "Cache Dir location: ", path);
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
