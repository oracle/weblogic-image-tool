// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "deleteEntry",
        description = "Command to delete a cache entry"
)
public class DeleteEntry extends CacheOperation {

    public DeleteEntry() {
    }

    public DeleteEntry(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {
        if (!Utils.isEmptyString(key)) {
            if (Constants.CACHE_DIR_KEY.equals(key.toLowerCase())) {
                return new CommandResponse(0, "Cannot delete key: " + key);
            } else if (Constants.DELETE_ALL_FOR_SURE.equalsIgnoreCase(key)) {
                Map<String, String> allEntries = cacheStore.getCacheItems();
                //allEntries.remove(CACHE_DIR_KEY);
                Map<String, String> deletedEntries = new HashMap<>();
                allEntries.forEach((k, v) -> deletedEntries.put(k, cacheStore.deleteFromCache(k)));
                return new CommandResponse(0, "Deleted all entries from cache", deletedEntries);
            } else {
                String oldValue = cacheStore.deleteFromCache(key);
                if (oldValue != null) {
                    return new CommandResponse(0, String.format("Deleted entry %s=%s", key, oldValue),
                            Collections.singletonMap(key, oldValue));
                } else {
                    return new CommandResponse(0, "Nothing to delete for key: " + key);
                }
            }
        }
        if (isCLIMode) {
            spec.commandLine().usage(System.out);
        }
        return new CommandResponse(-1, "Invalid arguments. --key should correspond to a valid entry in cache");
    }

    @Option(
            names = {"--key"},
            description = "Key corresponding to the cache entry to delete",
            required = true
    )
    private String key;
}
