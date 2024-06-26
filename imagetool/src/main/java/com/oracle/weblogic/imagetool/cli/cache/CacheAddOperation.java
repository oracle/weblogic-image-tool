// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

public abstract class CacheAddOperation extends CacheOperation {

    public abstract String getKey();

    CommandResponse addToCache() throws CacheStoreException {
        // if file is invalid or does not exist, return an error
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return CommandResponse.error("IMG-0049", filePath);
        }

        String key = getKey();
        // if the new value is the same as the existing cache value, do nothing
        String existingValue = cache().getValueFromCache(key);
        if (absolutePath().toString().equals(existingValue)) {
            return CommandResponse.success("IMG-0075");
        }

        // if there is already a cache entry and the user did not ask to force it, return an error
        if (!force && existingValue != null) {
            return CommandResponse.error("IMG-0048", key, existingValue);
        }

        // input appears valid, add the entry to the cache and exit
        cache().addToCache(key, absolutePath().toString());
        return CommandResponse.success("IMG-0050", key, cache().getValueFromCache(key));
    }

    private Path absolutePath() {
        if (absolutePath == null) {
            absolutePath = filePath.toAbsolutePath();
        }
        return absolutePath;
    }

    private Path absolutePath = null;

    @Option(
        names = {"--force"},
        description = "Overwrite existing entry, if it exists"
    )
    private boolean force = false;


    @Option(
        names = {"-p", "--path"},
        description = "Location of the file on disk. For ex: /path/to/patch-or-installer.zip",
        required = true
    )
    private Path filePath;
}
