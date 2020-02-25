// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

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
                return new CommandResponse(0, "IMG-0046");
            } else {
                String oldValue = cacheStore.deleteFromCache(key);
                if (oldValue != null) {
                    return new CommandResponse(0, "IMG-0051", key, oldValue);
                } else {
                    return new CommandResponse(0, "IMG-0052", key);
                }
            }
        }
        return new CommandResponse(-1, "IMG-0045");
    }

    @Option(
            names = {"--key"},
            description = "Key corresponding to the cache entry to delete",
            required = true
    )
    private String key;
}
