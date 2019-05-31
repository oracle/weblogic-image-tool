package com.oracle.weblogic.imagetool.impl.meta;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.util.Constants;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileCacheStoreTest {

    private CacheStore cacheStore = new CacheStoreFactory().get();

    @Test
    public void a3addToCache() {
        assertTrue(cacheStore.addToCache("abc_xyz_123", "this_is_a_test"));
    }

    @Test
    public void a4getValueFromCache() {
        assertEquals("this_is_a_test", cacheStore.getValueFromCache("abc_xyz_123"));
    }

    @Test
    public void a5hasMatchingKeyValue() {
        assertTrue(cacheStore.hasMatchingKeyValue("abc_xyz_123", "this_is_a_test"));
    }

    @Test
    public void a6deleteFromCache() {
        assertNull(cacheStore.deleteFromCache("non_existent_key"));
        assertEquals("this_is_a_test", cacheStore.deleteFromCache("abc_xyz_123"));
    }

    @Test
    public void a7getCacheItems() {
        assertNotNull(cacheStore.getCacheItems());
    }
}
