// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.util.HashMap;
import java.util.Map;

public class CacheStoreTestImpl implements CacheStore {

    HashMap<String, String> cache = new HashMap<>();

    @Override
    public String getCacheDir() {
        return null;
    }

    @Override
    public String getValueFromCache(String key) {
        return cache.get(key);
    }

    @Override
    public boolean hasMatchingKeyValue(String key, String value) {
        return false;
    }

    @Override
    public boolean addToCache(String key, String value) {
        cache.put(key, value);
        return true;
    }

    @Override
    public String deleteFromCache(String key) {
        return null;
    }

    @Override
    public Map<String, String> getCacheItems() {
        return cache;
    }
}
