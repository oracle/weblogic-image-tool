/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.impl;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.AbstractFile;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.util.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * This represents a WLS, JDK or WDT installer.
 */
public class InstallerFile extends AbstractFile {

    private InstallerType type;
    private final Logger logger = Logger.getLogger(InstallerFile.class.getName());

    public InstallerFile(CachePolicy cachePolicy, InstallerType type, String version, String userId, String password) {
        super(type.toString(), version, cachePolicy, userId, password);
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        // check entry exists in cache
        String filePath = cacheStore.getValueFromCache(getKey());
        switch (cachePolicy) {
            case ALWAYS:
                if (!isFileOnDisk(filePath)) {
                    throw new Exception("CachePolicy prohibits download. Please add cache entry for key: " + getKey());
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
        String key = getKey();
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
