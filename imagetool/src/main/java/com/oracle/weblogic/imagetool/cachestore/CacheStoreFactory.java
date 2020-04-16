// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.util.HashMap;
import java.util.Map;

import com.oracle.weblogic.imagetool.util.Constants;

/**
 * Provides access to a Cache Store.
 */
public class CacheStoreFactory {

    private static CacheStoreFactory factory;

    private Map<String, CacheStore> stores;

    private CacheStoreFactory() {
        stores = new HashMap<>();
        stores.put(Constants.FILE_CACHE, new FileCacheStore());
    }

    /**
     * Get the default file store cache.
     * @return the cached instance of the file cache.
     */
    public CacheStore defaultStore() {
        return stores.get(Constants.FILE_CACHE);
    }

    /**
     * Get the default cache store.
     * @return the cached instance of the file cache store
     */
    public static CacheStore get() {
        if (factory == null) {
            factory = new CacheStoreFactory();
        }

        return factory.defaultStore();
    }
}
