// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;

public class FileCacheStore implements CacheStore {

    public static final String CACHE_DIR_ENV = "WLSIMG_CACHEDIR";
    private static final LoggingFacade logger = LoggingFactory.getLogger(FileCacheStore.class);

    private final Properties properties = new Properties();
    private final File metadataFile;
    private final String cacheDir;

    FileCacheStore() throws CacheStoreException {
        try {
            cacheDir = initCacheDir();
            metadataFile = Paths.get(cacheDir, Constants.DEFAULT_META_FILE).toFile();
            if (metadataFile.exists() && metadataFile.isFile()) {
                loadProperties(metadataFile);
            } else {
                if (!metadataFile.createNewFile()) {
                    throw new IOException("Failed to create file cache metadata file " + metadataFile.getName());
                }
            }
        } catch (IOException e) {
            CacheStoreException error =
                new CacheStoreException("Failed to establish a cache store on the filesystem", e);
            logger.throwing(error);
            throw error;
        }
    }

    @Override
    public String getCacheDir() {
        return cacheDir;
    }

    @Override
    public String getValueFromCache(String key) {
        Objects.requireNonNull(key, Utils.getMessage("IMG-0066"));
        return properties.getProperty(key.toLowerCase());
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        return properties.containsKey(key.toLowerCase());
    }

    @Override
    public void addToCache(String key, String value) throws CacheStoreException {
        Objects.requireNonNull(key, Utils.getMessage("IMG-0066"));
        Objects.requireNonNull(value, "Cache item value cannot be null");
        properties.put(key.toLowerCase(), value);
        persistToDisk();
    }

    @Override
    public String deleteFromCache(String key) throws CacheStoreException {
        Objects.requireNonNull(key, Utils.getMessage("IMG-0066"));
        String oldValue = (String) properties.remove(key.toLowerCase());
        if (oldValue != null) {
            persistToDisk();
        }
        return oldValue;
    }

    @Override
    public void clearCache() throws CacheStoreException {
        properties.clear();
        persistToDisk();
    }

    @Override
    public Map<String, String> getCacheItems() {
        Stream<Map.Entry<Object, Object>> stream = properties.entrySet().stream();
        return stream.collect(Collectors.toMap(
            e -> String.valueOf(e.getKey()),
            e -> String.valueOf(e.getValue())));
    }

    private void persistToDisk() throws CacheStoreException {
        logger.entering();
        synchronized (properties) {
            try (FileOutputStream outputStream = new FileOutputStream(metadataFile)) {
                properties.store(outputStream, "changed on:" + LocalDateTime.now());
            } catch (IOException e) {
                CacheStoreException error = new CacheStoreException("Could not persist cache file", e);
                logger.throwing(error);
                throw error;
            }
        }
        logger.exiting();
    }

    private void loadProperties(File propsFile) {
        logger.entering();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(propsFile))) {
            if (properties.isEmpty()) {
                properties.load(bufferedReader);
            } else {
                Properties tmpProperties = new Properties();
                tmpProperties.load(bufferedReader);
                tmpProperties.forEach((key, value) -> properties.put(((String) key).toLowerCase(), value));
            }
        } catch (IOException e) {
            // it is okay to fail, the constructor will attempt to create a new one
            logger.fine("Failed to load properties file", e);
        }
        logger.exiting();
    }

    private static String defaultCacheDir() {
        return System.getProperty("user.home") + File.separator + "cache";
    }

    /**
     * Initialize the cache store directory.
     *
     * @return cache directory
     */
    private static String initCacheDir() throws IOException {
        String cacheDirStr = Utils.getEnvironmentProperty(CACHE_DIR_ENV, FileCacheStore::defaultCacheDir);

        Path cacheDir = Paths.get(cacheDirStr);

        boolean pathExists = Files.exists(cacheDir, LinkOption.NOFOLLOW_LINKS);

        if (!pathExists) {
            Files.createDirectory(cacheDir);
        } else {
            if (!Files.isDirectory(cacheDir)) {
                throw new IOException("Cache Directory specified is not a directory " + cacheDirStr);
            }
            if (!Files.isWritable(cacheDir)) {
                throw new IOException("Cache Directory specified is not writable " + cacheDirStr);
            }
        }

        return cacheDirStr;
    }
}
