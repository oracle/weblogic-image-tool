/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.impl.meta;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CacheStoreFactory implements Supplier<CacheStore> {

    private final static Map<String, CacheStore> cashStoreMap = new HashMap<>();

    static {
        cashStoreMap.put(Constants.FILE_CACHE, FileCacheStore.CACHE_STORE);
    }

    public CacheStore getCacheStore(String backingType) {
        return cashStoreMap.getOrDefault(backingType.toUpperCase(), FileCacheStore.CACHE_STORE);
    }

    @Override
    public CacheStore get() {
        return FileCacheStore.CACHE_STORE;
    }
}
