// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore.meta;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@TestMethodOrder(Alphanumeric.class)
class FileCacheStoreTest {

    private CacheStore cacheStore = new CacheStoreFactory().get();

    @Test
    void a3addToCache() {
        assertTrue(cacheStore.addToCache("abc_xyz_123", "this_is_a_test"));
    }

    @Test
    void a4getValueFromCache() {
        assertEquals("this_is_a_test", cacheStore.getValueFromCache("abc_xyz_123"));
    }

    @Test
    void a5hasMatchingKeyValue() {
        assertTrue(cacheStore.hasMatchingKeyValue("abc_xyz_123", "this_is_a_test"));
    }

    @Test
    void a6deleteFromCache() {
        assertNull(cacheStore.deleteFromCache("non_existent_key"));
        assertEquals("this_is_a_test", cacheStore.deleteFromCache("abc_xyz_123"));
    }

    @Test
    void a7getCacheItems() {
        assertNotNull(cacheStore.getCacheItems());
    }
}
