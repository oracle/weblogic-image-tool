// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
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

public class FileCacheStore implements CacheStore {

    private static final LoggingFacade logger = LoggingFactory.getLogger(FileCacheStore.class);
    public static final String CACHEDIR = "WLSIMG_CACHEDIR";

    private final Properties properties = new Properties();
    private String metadataPath;

    FileCacheStore() {
        try {
            String userCacheDir = initCacheDir();
            metadataPath = userCacheDir + File.separator + Constants.DEFAULT_META_FILE;
            File metadataFile = new File(metadataPath);
            if (metadataFile.exists() && metadataFile.isFile()) {
                loadProperties(metadataFile);
            } else {
                if (!metadataFile.createNewFile()) {
                    throw new IOException("Failed to create file cache metadata file " + metadataFile.getName());
                }
            }
            if (properties.getProperty(Constants.CACHE_DIR_KEY) == null) {
                properties.put(Constants.CACHE_DIR_KEY, userCacheDir);
                persistToDisk();
            }
            File cacheDir = new File(properties.getProperty(Constants.CACHE_DIR_KEY));
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                // the cache directory did not exist, and the mkdirs failed to create it
                throw new IOException("Failed to create cache directory: " + cacheDir.getName());
            }
        } catch (IOException e) {
            logger.severe("Failed to establish a cache store on the filesystem", e);
            System.exit(-1);
        }
    }

    @Override
    public String getCacheDir() {
        return properties.getProperty(Constants.CACHE_DIR_KEY);
    }

    @Override
    public String getValueFromCache(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return properties.getProperty(key.toLowerCase());
    }

    @Override
    public boolean hasMatchingKeyValue(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        return value.equals(properties.getProperty(key.toLowerCase()));
    }

    @Override
    public boolean addToCache(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "Cache item value cannot be null");
        properties.put(key.toLowerCase(), value);
        return persistToDisk();
    }

    @Override
    public String deleteFromCache(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        if (Constants.CACHE_DIR_KEY.equalsIgnoreCase(key)) {
            return properties.getProperty(Constants.CACHE_DIR_KEY, null);
        }
        String oldValue = (String) properties.remove(key.toLowerCase());
        if (oldValue != null) {
            persistToDisk();
        }
        return oldValue;
    }

    @Override
    public Map<String, String> getCacheItems() {
        Stream<Map.Entry<Object, Object>> stream = properties.entrySet().stream();
        return stream.collect(Collectors.toMap(
            e -> String.valueOf(e.getKey()),
            e -> String.valueOf(e.getValue())));
    }

    private boolean persistToDisk() {
        logger.entering();
        boolean retVal = true;
        synchronized (properties) {
            try (FileOutputStream outputStream = new FileOutputStream(metadataPath)) {
                properties.store(outputStream, "changed on:" + LocalDateTime.now());
            } catch (IOException e) {
                retVal = false;
                logger.fine("Could not persist cache file", e);
            }
        }
        logger.exiting(retVal);
        return retVal;
    }

    private void loadProperties(File propsFile) {
        logger.entering();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(propsFile))) {
            if (properties.isEmpty()) {
                properties.load(bufferedReader);
            } else {
                Properties tmpProperties = new Properties();
                tmpProperties.load(bufferedReader);
                // Do not let cache.dir to be modified outside setCacheDir
                tmpProperties.remove(Constants.CACHE_DIR_KEY);
                tmpProperties.forEach((key, value) -> properties.put(((String) key).toLowerCase(), value));
            }
        } catch (IOException e) {
            // it is okay to fail, the constructor will attempt to create a new one
            logger.fine("Failed to load properties file", e);
        }
        logger.exiting();
    }

    /**
     * Initialize the cache store directory.
     *
     * @return cache directory
     */
    private static String initCacheDir() throws IOException {
        String cacheDirStr = System.getenv(CACHEDIR);
        if (cacheDirStr == null) {
            cacheDirStr = System.getProperty(CACHEDIR);
        }
        if (cacheDirStr == null) {
            cacheDirStr = System.getProperty("user.home") + "/cache";
        }
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
