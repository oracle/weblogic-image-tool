package com.oracle.weblogicx.imagebuilder.builder.impl;

import com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver;
import com.oracle.weblogicx.imagebuilder.builder.api.model.AbstractFile;
import com.oracle.weblogicx.imagebuilder.builder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.builder.util.HttpUtil;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver.CACHE_KEY_SEPARATOR;

public class InstallerFile extends AbstractFile {

    private boolean tryToDownload;
    private String userId;
    private String password;
    private InstallerType type = null;

    public InstallerFile(String key, boolean tryToDownload) {
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

    @Override
    public String resolve(MetaDataResolver metaDataResolver) throws Exception {
        // check entry exists in cache
        String filePath = metaDataResolver.getValueFromCache(key);
        // check if the file exists on disk
        if (!isFileOnDisk(filePath)) {
            if (tryToDownload) {
                String urlPath = metaDataResolver.getValueFromCache(key + "_url");
                if (urlPath != null) {
                    String fileName = new URL(urlPath).getPath();
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    String targetFilePath = metaDataResolver.getCacheDir() + File.separator + key +
                            File.separator + fileName;
                    new File(targetFilePath).getParentFile().mkdirs();
                    System.out.println("1. Downloading from " + urlPath + " to " + targetFilePath);
                    HttpUtil.downloadFile(urlPath, targetFilePath, userId, password);
                    metaDataResolver.addToCache(key, targetFilePath);
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

    public List<String> getBuildArg(String location) {
        if (type != null) {
            return type.getBuildArg(location);
        }
        return Collections.emptyList();
    }
}
