// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.impl.meta;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;

public enum FileCacheStore implements CacheStore {

    CACHE_STORE;

    private final Properties properties = new Properties();
    private String metadataPath;

    FileCacheStore() {
        try {
            String userCacheDir = Utils.getCacheDir();
            metadataPath = String.format("%s%s%s", userCacheDir, File.separator, Constants.DEFAULT_META_FILE);
            File metadataFile = new File(metadataPath);
            if (metadataFile.exists() && metadataFile.isFile()) {
                loadProperties(metadataFile);
            } else {
                metadataFile.getParentFile().mkdirs();
                metadataFile.createNewFile();
            }
            if (properties.getProperty(Constants.CACHE_DIR_KEY) == null) {
                properties.put(Constants.CACHE_DIR_KEY, userCacheDir);
                persistToDisk();
            }
            File cacheDir = new File(properties.getProperty(Constants.CACHE_DIR_KEY));
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        if (Constants.CACHE_DIR_KEY.equals(key.toLowerCase())) {
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
        boolean retVal = true;
        synchronized (properties) {
            try (FileOutputStream outputStream = new FileOutputStream(metadataPath)) {
                properties.store(outputStream, "changed on:" + LocalDateTime.now());
            } catch (IOException e) {
                retVal = false;
                e.printStackTrace();
            }
        }
        return retVal;
    }

    private void loadProperties(File propsFile) {
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
            e.printStackTrace();
        }
    }


}
