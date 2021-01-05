// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CacheStoreTestImpl implements CacheStore {

    private HashMap<String, String> cache = new HashMap<>();
    private Path cacheDir;

    public CacheStoreTestImpl(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public String getCacheDir() {
        return cacheDir.toString();
    }

    @Override
    public String getValueFromCache(String key) {
        return cache.get(key);
    }

    @Override
    public boolean hasMatchingKeyValue(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        return value.equals(cache.get(key.toLowerCase()));
    }

    @Override
    public void addToCache(String key, String value) {
        cache.put(key.toLowerCase(), value);
    }

    @Override
    public String deleteFromCache(String key) {
        return null;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public Map<String, String> getCacheItems() {
        return cache;
    }
}
