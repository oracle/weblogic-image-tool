// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

/**
 * Provides access to a Cache Store.
 */
public class CacheStoreFactory {

    private static CacheStore store;

    private CacheStoreFactory() {
        // hidden constructor
    }

    /**
     * Get the cache store.
     * @return the cached instance of the file cache store
     */
    public static CacheStore cache() throws CacheStoreException {
        if (store == null) {
            store = new FileCacheStore();
        }

        return store;
    }
}
