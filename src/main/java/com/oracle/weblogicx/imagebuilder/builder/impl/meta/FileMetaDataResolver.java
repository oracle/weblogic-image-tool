/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.impl.meta;

import com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FileMetaDataResolver implements MetaDataResolver {

    META_RESOLVER;

    private static final Properties properties = new Properties();
    private static String metadataPath;
    private static final String DEFAULT_CACHE_DIR = Paths.get(System.getProperty("user.home"), "cache")
            .toAbsolutePath().toString();
    private static final String DEFAULT_META_PATH = DEFAULT_CACHE_DIR + File.separator + ".metadata";
    static {
        try {
            final Preferences preferences = Preferences.userNodeForPackage(META_RESOLVER.getClass());
            metadataPath = preferences.get("metadata.file", null);
            if (metadataPath == null || metadataPath.isEmpty()) {
                metadataPath = DEFAULT_META_PATH;
                preferences.put("metadata.file", metadataPath);
                preferences.flush();
            }
            File metadataFile = new File(metadataPath);
            if ( metadataFile.exists() && metadataFile.isFile()) {
                loadProperties(metadataFile);
            } else {
                metadataFile.getParentFile().mkdirs();
                metadataFile.createNewFile();
            }
            if (properties.getProperty("cache.dir") == null) {
                saveProperties("cache.dir", DEFAULT_CACHE_DIR);
            }
            File cacheDir = new File(properties.getProperty("cache.dir"));
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public String getCacheDir() {
        return properties.getProperty("cache.dir", DEFAULT_CACHE_DIR);
    }

    @Override
    public Optional<String> getValueFromCache(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    @Override
    public boolean hasMatchingKeyValue(String key, String value) {
        if (key == null || value == null) {
            return false;
        }
        return value.equalsIgnoreCase(properties.getProperty(key));
    }

    @Override
    public boolean addToCache(String key, String value) throws IllegalArgumentException {
        Objects.requireNonNull(value, "Cache item value cannot be null");
        return saveProperties(key, value);
    }

    @Override
    public Map<String, String> getCacheItems() {
        Stream<Map.Entry<Object, Object>> stream = properties.entrySet().stream();
        return stream.collect(Collectors.toMap(
                e -> String.valueOf(e.getKey()),
                e -> String.valueOf(e.getValue())));
    }

    private static boolean saveProperties(String key, String value) {
        boolean retVal = true;
        synchronized (properties) {
            //caller checks for null key, value
            properties.put(key, value);
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
                tmpProperties.forEach(properties::put);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCacheDir(String cacheDirPath) {
        if (cacheDirPath != null && !cacheDirPath.equals(getCacheDir())) {
            saveProperties("cache.dir", cacheDirPath);
        }
    }

}
