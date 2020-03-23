// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.oracle.weblogic.imagetool.util.Constants;

public class CacheStoreFactory implements Supplier<CacheStore> {

    private static final Map<String, CacheStore> cashStoreMap = new HashMap<>();

    static {
        cashStoreMap.put(Constants.FILE_CACHE, FileCacheStore.CACHE_STORE);
    }

    @Override
    public CacheStore get() {
        return FileCacheStore.CACHE_STORE;
    }
}
