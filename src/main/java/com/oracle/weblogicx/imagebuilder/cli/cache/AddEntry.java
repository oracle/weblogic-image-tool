/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addEntry",
        description = "Command to add a cache entry. Use caution"
)
public class AddEntry extends CacheOperation {

    @Override
    public CommandResponse call() throws Exception {
        if (key != null && !key.isEmpty() && location != null && !location.isEmpty()) {
            String oldValue = cacheStore.getValueFromCache(key);
            String msg;
            if (oldValue != null) {
                msg = String.format("Replaced old value %s with new value %s for key %s", oldValue, location, key);
            } else {
                msg = String.format("Added entry %s=%s", key, location);
            }
            if (cacheStore.addToCache(key, location)) {
                return new CommandResponse(0, msg);
            } else {
                return new CommandResponse(-1, "Command Failed");
            }
        }
        if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
            spec.commandLine().usage(System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key & --path required.");
    }

    @Option(
            names = {"--key"},
            description = "Key for the cache entry",
            required = true
    )
    private String key;

    @Option(
            names = {"--path"},
            description = "Value for the cache entry",
            required = true
    )
    private String location;

//    private CacheStore cacheStore = new CacheStoreFactory().get();
//
//    @Unmatched
//    List<String> unmatchedOptions;
//
//    @Spec
//    CommandSpec spec;
}
