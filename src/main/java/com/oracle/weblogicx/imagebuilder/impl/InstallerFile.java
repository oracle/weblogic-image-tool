package com.oracle.weblogicx.imagebuilder.impl;

import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.api.model.AbstractFile;
import com.oracle.weblogicx.imagebuilder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.util.HttpUtil;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.oracle.weblogicx.imagebuilder.api.meta.CacheStore.CACHE_KEY_SEPARATOR;

/**
 * This represents a WLS, JDK or WDT installer.
 */
public class InstallerFile extends AbstractFile {

    private boolean tryToDownload;
    private String userId;
    private String password;
    private InstallerType type = null;
    private final Logger logger = Logger.getLogger(InstallerFile.class.getName());

    private InstallerFile(String key, boolean tryToDownload) {
        super(key);
        this.tryToDownload = tryToDownload;
    }

    public InstallerFile(String key, boolean tryToDownload, String userId, String password) {
        this(key, tryToDownload);
        this.userId = userId;
        this.password = password;
    }

    public InstallerFile(InstallerType type, String version, boolean tryToDownload, String userId, String password) {
        this(type.toString() + CACHE_KEY_SEPARATOR + version, tryToDownload, userId, password);
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        // check entry exists in cache
        String filePath = cacheStore.getValueFromCache(key);
        // check if the file exists on disk
        if (!isFileOnDisk(filePath)) {
            if (tryToDownload) {
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
                    throw new Exception("Cannot find download link for entry " + key + "_url in cache");
                }
            } else {
                //not allowed to download
                throw new Exception("CachePolicy prohibits download. Please add cache entry for key: " + key);
            }
        }
        return filePath;
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
