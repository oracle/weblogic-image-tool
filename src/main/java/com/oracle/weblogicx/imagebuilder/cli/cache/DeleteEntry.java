/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "deleteEntry",
        description = "Command to delete a cache entry"
)
public class DeleteEntry extends CacheOperation {

    @Override
    public CommandResponse call() throws Exception {
        if (key != null && !key.isEmpty()) {
            if (Constants.CACHE_DIR_KEY.equals(key.toLowerCase())) {
                return new CommandResponse(-1, "Error: Cannot delete cache.dir entry. Use setCacheDir instead");
            }
            String oldValue = cacheStore.deleteFromCache(key);
            if (oldValue != null) {
                return new CommandResponse(0, String.format("Deleted entry %s=%s", key, oldValue),
                        oldValue);
            } else {
                return new CommandResponse(0, "Nothing to delete for key: " + key);
            }
        }
        if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
            spec.commandLine().usage(System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key should correspond to a valid entry in cache");
    }

    @Option(
            names = { "--key" },
            description = "Key corresponding to the cache entry to delete",
            required = true
    )
    private String key;

//    @Spec
//    CommandSpec spec;
//
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
