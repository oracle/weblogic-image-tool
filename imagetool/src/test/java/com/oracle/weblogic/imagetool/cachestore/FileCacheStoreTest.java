// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileCacheStoreTest {

    private static final String testKey = "abc_xyz_123";
    private static final String testVal = "this_is_a_test";

    @BeforeAll
    static void init(@TempDir File tempDir) throws CacheStoreException {
        System.setProperty(FileCacheStore.CACHEDIR, tempDir.getAbsolutePath());
        cache().clearCache();
    }

    @Test
    @Order(1)
    void addingValueToCache() {
        // add value to cache
        assertDoesNotThrow(() -> cache().addToCache(testKey, testVal), "Add to cache threw an exception");
    }

    @Test
    @Order(2)
    void checkValueInCache() {
        // check to see if the key that was just added is there, and value matches expected value
        assertDoesNotThrow(() ->
                assertTrue(cache().containsKey(testKey)),
            "containsKey failed to find key or value that was just added");

        // check (another way) that the value was just added
        assertDoesNotThrow(() ->
                assertEquals(testVal, cache().getValueFromCache(testKey), "Found unexpected value in cache"),
            "Get from cache threw an exception");
    }

    @Test
    @Order(3)
    void deleteValueFromCache() {
        assertDoesNotThrow(() ->
                assertNull(cache().deleteFromCache("non_existent_key"),
                    "Deleting non-existent key should not have a value"),
            "Delete from cache threw an exception");

        assertDoesNotThrow(() ->
                assertEquals(testVal, cache().deleteFromCache(testKey), "Value from deleted key did not match"),
            "Delete from cache threw an exception");
    }

    @Test
    @Order(4)
    void verifyCacheSize() {
        assertDoesNotThrow(() ->
            assertNotNull(cache().getCacheItems(), "Get cache items should never be null"),
            "getCacheItems threw an exception");

        assertDoesNotThrow(() ->
                assertEquals(0, cache().getCacheItems().size(), "Get cache items should never be null"),
            "getCacheItems threw an exception");
    }
}
