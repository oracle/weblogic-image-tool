/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.impl.meta;

import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.oracle.weblogicx.imagebuilder.util.Constants.CACHE_STORE_TYPE;
import static com.oracle.weblogicx.imagebuilder.util.Constants.FILE_CACHE;
import static com.oracle.weblogicx.imagebuilder.util.Constants.PREF_CACHE;

public class CacheStoreFactory implements Supplier<CacheStore> {

    private final static Map<String, CacheStore> cashStoreMap = new HashMap<>();

    static {
        cashStoreMap.put(FILE_CACHE, FileCacheStore.CACHE_STORE);
        cashStoreMap.put(PREF_CACHE, PreferenceCacheStore.CACHE_STORE);
    }

    public CacheStore getCacheStore(String backingType) {
        return cashStoreMap.getOrDefault(backingType.toUpperCase(), FileCacheStore.CACHE_STORE);
    }

    @Override
    public CacheStore get() {
        String backingType = null;
        try {
            backingType = System.getenv(CACHE_STORE_TYPE);
            backingType = Utils.isEmptyString(backingType) ? System.getProperty(CACHE_STORE_TYPE, FILE_CACHE) : backingType;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getCacheStore(backingType);
    }
}
