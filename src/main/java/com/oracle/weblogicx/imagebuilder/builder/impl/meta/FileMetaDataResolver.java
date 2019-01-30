/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.impl.meta;

import com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.CACHE_DIR_KEY;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_META_FILE;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.METADATA_PREF_KEY;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.WEBLOGICX_IMAGEBUILDER;

public enum FileMetaDataResolver implements MetaDataResolver {

    META_RESOLVER;

    private static final Properties properties = new Properties();
    private static String metadataPath;
    private static final String DEFAULT_CACHE_DIR = Paths.get(System.getProperty("user.home"), "cache")
            .toAbsolutePath().toString();
    private static final String DEFAULT_META_PATH = DEFAULT_CACHE_DIR + File.separator + DEFAULT_META_FILE;
    private static final Preferences preferences = Preferences.userRoot().node(WEBLOGICX_IMAGEBUILDER);

    static {
        try {
//            final Preferences preferences = Preferences.userNodeForPackage(META_RESOLVER.getClass());
            metadataPath = preferences.get(METADATA_PREF_KEY, null);
            if (metadataPath == null || metadataPath.isEmpty()) {
                metadataPath = DEFAULT_META_PATH;
                preferences.put(METADATA_PREF_KEY, metadataPath);
                preferences.flush();
            }
            File metadataFile = new File(metadataPath);
            if ( metadataFile.exists() && metadataFile.isFile()) {
                loadProperties(metadataFile);
            } else {
                metadataFile.getParentFile().mkdirs();
                metadataFile.createNewFile();
            }
            if (properties.getProperty(CACHE_DIR_KEY) == null) {
                properties.put(CACHE_DIR_KEY, DEFAULT_CACHE_DIR);
                persistToDisk();
            }
            File cacheDir = new File(properties.getProperty(CACHE_DIR_KEY));
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
        return properties.getProperty(CACHE_DIR_KEY, DEFAULT_CACHE_DIR);
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
        if (CACHE_DIR_KEY.equals(key.toLowerCase())) {
            return null;
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

    private static boolean persistToDisk() {
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

    private static void loadProperties(File propsFile) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(propsFile))) {
            if (properties.isEmpty()) {
                properties.load(bufferedReader);
            } else {
                System.out.println("******* Can this be? ******");
                Properties tmpProperties = new Properties();
                tmpProperties.load(bufferedReader);
                tmpProperties.forEach((key, value) -> properties.put(((String) key).toLowerCase(), value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean setCacheDir(String cacheDirPath) {
        if (cacheDirPath != null && !cacheDirPath.equals(getCacheDir())) {
            properties.put(CACHE_DIR_KEY, cacheDirPath);
            try {
                metadataPath = getCacheDir() + File.separator + DEFAULT_META_FILE;
                File metaDataFile = new File(metadataPath);
                if (metaDataFile.exists() && metaDataFile.isFile()) {
                    loadProperties(metaDataFile);
                } else {
                    metaDataFile.getParentFile().mkdirs();
                    metaDataFile.createNewFile();
                }
                preferences.put(METADATA_PREF_KEY, metadataPath);
                preferences.flush();
                return persistToDisk();
            } catch (BackingStoreException | IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
