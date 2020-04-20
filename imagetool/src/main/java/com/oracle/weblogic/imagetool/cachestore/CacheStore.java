// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.util.Map;

/**
 * This is the interface that helps keep track of application metadata like
 * which patches have been downloaded and their location on disk.
 */
public interface CacheStore {

    String CACHE_KEY_SEPARATOR = "_";

    /**
     * Cache dir used by this application. cache dir is where the application downloads
     * artifacts to.
     *
     * @return cache dir location on disk
     */
    String getCacheDir();

    /**
     * Returns the value if key is present. Since the application tracks downloaded artifact location,
     * key is usually patch number or artifact identifier. Value is location on disk.
     *
     * @param key key to look for. Ex: patch number
     * @return value if present in cache or else null.
     */
    String getValueFromCache(String key);

    /**
     * Checks if cache has certain key, value combination. This is used to check if a certain artifact
     * is in the desired location if it has been downloaded previously.
     *
     * @param key   artifact identifier
     * @param value location on disk
     * @return true if found
     */
    boolean hasMatchingKeyValue(String key, String value);

    /**
     * Add an entry to the cache metadata file.
     *
     * @param key   artifact identifier
     * @param value a file path
     */
    void addToCache(String key, String value) throws CacheStoreException;

    /**
     * Delete an entry from the cache.
     *
     * @param key key corresponding to an entry in the cache
     * @return value or null
     */
    String deleteFromCache(String key) throws CacheStoreException;

    /**
     * Delete all entries from the cache.
     */
    void clearCache() throws CacheStoreException;

    /**
     * Returns a map of current items in the cache.
     *
     * @return map of current items
     */
    Map<String, String> getCacheItems();

}
