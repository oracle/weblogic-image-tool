/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addEntry",
        description = "Command to add a cache entry. Use caution"
)
public class AddEntry extends CacheOperation {

    public AddEntry() {
    }

    public AddEntry(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {
        if (!Utils.isEmptyString(key) && !Utils.isEmptyString(location)) {
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
        if (isCLIMode) {
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
}
