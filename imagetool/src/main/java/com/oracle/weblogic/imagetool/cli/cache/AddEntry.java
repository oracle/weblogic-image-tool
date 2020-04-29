// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

@Command(
        name = "addEntry",
        description = "Command to add a cache entry. Use caution"
)
public class AddEntry extends CacheOperation {

    @Override
    public CommandResponse call() throws CacheStoreException {
        if (!Utils.isEmptyString(key) && !Utils.isEmptyString(location)) {
            String oldValue = cache().getValueFromCache(key);
            String msg;
            if (oldValue != null) {
                msg = String.format("Replaced old value %s with new value %s for key %s", oldValue, location, key);
            } else {
                msg = String.format("Added entry %s=%s", key, location);
            }
            cache().addToCache(key, location);
            return new CommandResponse(0, msg);
        }
        return new CommandResponse(-1, "IMG-0044");
    }

    @Option(
            names = {"--key"},
            description = "Key for the cache entry",
            required = true
    )
    private String key;

    @Option(
            names = {"--value"},
            description = "Value for the cache entry",
            required = true
    )
    private String location;
}
