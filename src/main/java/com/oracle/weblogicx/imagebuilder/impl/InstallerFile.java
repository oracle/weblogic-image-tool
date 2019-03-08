/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.impl;

import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.api.model.AbstractFile;
import com.oracle.weblogicx.imagebuilder.api.model.CachePolicy;
import com.oracle.weblogicx.imagebuilder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.oracle.weblogicx.imagebuilder.api.meta.CacheStore.CACHE_KEY_SEPARATOR;

/**
 * This represents a WLS, JDK or WDT installer.
 */
public class InstallerFile extends AbstractFile {

    private InstallerType type;
    private final Logger logger = Logger.getLogger(InstallerFile.class.getName());

    public InstallerFile(CachePolicy cachePolicy, InstallerType type, String version, String userId, String password) {
        super(type.toString() + CACHE_KEY_SEPARATOR + version, cachePolicy, userId, password);
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        // check entry exists in cache
        String filePath = cacheStore.getValueFromCache(key);
        switch (cachePolicy) {
            case ALWAYS:
                if (!isFileOnDisk(filePath)) {
                    throw new Exception("CachePolicy prohibits download. Please add cache entry for key: " + key);
                }
                break;
            case FIRST:
                if (!isFileOnDisk(filePath)) {
                    filePath = downloadInstaller(cacheStore);
                }
                break;
            case NEVER:
                filePath = downloadInstaller(cacheStore);
                break;
        }
        return filePath;
    }

    private String downloadInstaller(CacheStore cacheStore) throws IOException {
        String urlPath = cacheStore.getValueFromCache(key + "_url");
        if (urlPath != null) {
            String fileName = new URL(urlPath).getPath();
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            String targetFilePath = cacheStore.getCacheDir() + File.separator + key +
                    File.separator + fileName;
            new File(targetFilePath).getParentFile().mkdirs();
            logger.info("Downloading from " + urlPath + " to " + targetFilePath);
            HttpUtil.downloadFile(urlPath, targetFilePath, userId, password);
            cacheStore.addToCache(key, targetFilePath);
            return targetFilePath;
        } else {
            throw new IOException("Cannot find download link for entry " + key + "_url in cache");
        }
    }

    /**
     * Constructs the build-arg required to pass to the docker build
     *
     * @param location path to installer on local disk
     * @return list of args
     */
    public List<String> getBuildArg(String location) {
        if (type != null) {
            return type.getBuildArg(location);
        }
        return Collections.emptyList();
    }
}
