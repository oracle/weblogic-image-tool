/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.impl;

import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.api.model.AbstractFile;
import com.oracle.weblogicx.imagebuilder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.util.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static com.oracle.weblogicx.imagebuilder.api.meta.CacheStore.CACHE_KEY_SEPARATOR;

public class PatchFile extends AbstractFile {

    private String userId;
    private String password;
    private String patchId;
    private String category;
    private String version;
    private final Logger logger = Logger.getLogger(PatchFile.class.getName());

//    public PatchFile(String key) {
//        super(key);
//    }

//    private PatchFile(String key, String userId, String password) {
//        this(key);
//        this.userId = userId;
//        this.password = password;
//    }

    public PatchFile(String category, String version, String patchId, String userId, String password) {
        super(null);
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        this.category = category;
        this.version = version;
        this.patchId = patchId;
        this.userId = userId;
        this.password = password;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws Exception {

        String releaseNumber = ARUUtil.getReleaseNumber(category, version, userId, password);
        if (Utils.isEmptyString(patchId)) {
            patchId = ARUUtil.getLatestPSUNumber(category, version, userId, password);
            if (Utils.isEmptyString(patchId)) {
                throw new Exception(String.format("Failed to find latest psu for product category %s, version %s",
                        category, version));
            }
        }
        key = patchId + CACHE_KEY_SEPARATOR + releaseNumber;
        String filePath = cacheStore.getValueFromCache(key);

        if (filePath == null || !Files.isRegularFile(Paths.get(filePath))) {
            // try downloading it
            List<String> patches = ARUUtil.getPatchesFor(category, version, Collections.singletonList(patchId),
                    userId, password, cacheStore.getCacheDir());
            patches.forEach(x -> cacheStore.addToCache(x.substring(0, x.indexOf('=')), x.substring(x.indexOf('=') + 1)));
            if (!patches.isEmpty()) {
                key = patches.get(0).substring(0, patches.get(0).indexOf('='));
            }
            filePath = cacheStore.getValueFromCache(key);
            if (filePath == null || !Files.isRegularFile(Paths.get(filePath))) {
                throw new Exception(String.format("Failed to find patch %s for product category %s, version %s",
                        patchId, category, version));
            }
        } else {
            logger.info("Found matching cache entry: key=" + key + ", value=" + filePath);
        }
        return filePath;
    }
}
