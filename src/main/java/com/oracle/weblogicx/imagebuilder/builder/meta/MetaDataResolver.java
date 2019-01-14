package com.oracle.weblogicx.imagebuilder.builder.meta;

import java.util.Map;
import java.util.Optional;

/**
 * This is the interface that helps keep track of application metadata like
 * which patches have been downloaded and their location on disk.
 */
public interface MetaDataResolver {

    /**
     * Cache dir used by this application. cache dir is where the application downloads
     * artifacts to.
     * @return cache dir location on disk
     */
    String getCacheDir();

    /**
     * Returns the value if key is present. Since the application tracks downloaded artifact location,
     * key is usually patch number or artifact identifier. Value is location on disk.
     * @param key key to look for. Ex: patch number
     * @return value if present in cache or else null.
     */
    Optional<String> getValueFromCache(String key);

    /**
     * Checks if cache has certain key, value combination. This is used to check if a certain artifact
     * is in the desired location if it has been downloaded previously.
     * @param key artifact identifier
     * @param value location on disk
     * @return true if found
     */
    boolean hasMatchingKeyValue(String key, String value);

    /**
     * Add an entry to the cache metadata file.
     * @param key artifact identifier
     * @param value a file path
     * @return true if add is successful
     */
    boolean addToCache(String key, String value);

    /**
     * Returns a map of current items in the cache
     * @return map of current items
     */
    Map<String, String> getCacheItems();
}
