// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheStoreTestImpl implements CacheStore {

    private final HashMap<String, String> cache = new HashMap<>();
    private final Path cacheDir;

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
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        return cache.containsKey(key.toLowerCase());
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
        cache.clear();
    }

    @Override
    public Map<String, String> getCacheItems() {
        return cache;
    }

    @Override
    public List<String> getKeysForType(String type) {
        return new ArrayList<>();
    }
}
