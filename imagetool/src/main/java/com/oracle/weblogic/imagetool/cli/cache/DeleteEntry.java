// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

@Command(
        name = "deleteEntry",
        description = "Command to delete a cache entry"
)
public class DeleteEntry extends CacheOperation {

    @Override
    public CommandResponse call() throws Exception {
        if (!Utils.isEmptyString(key)) {
            if (Constants.CACHE_DIR_KEY.equalsIgnoreCase(key)) {
                return new CommandResponse(0, "Cannot delete key: " + key);
            } else if (Constants.DELETE_ALL_FOR_SURE.equalsIgnoreCase(key)) {
                cache().clearCache();
                return new CommandResponse(0, "IMG-0046");
            } else {
                String oldValue = cache().deleteFromCache(key);
                if (oldValue != null) {
                    return new CommandResponse(0, "IMG-0051", key, oldValue);
                } else {
                    return new CommandResponse(0, "IMG-0052", key);
                }
            }
        }
        return new CommandResponse(1, "IMG-0045");
    }

    @Option(
            names = {"--key"},
            description = "Key corresponding to the cache entry to delete",
            required = true
    )
    private String key;
}
