// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class PatchFile extends CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);

    private final AruPatch aruPatch;
    private final String userId;
    private final String password;

    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param aruPatch Patch metadata from ARU
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public PatchFile(AruPatch aruPatch, String userId, String password) {
        super(aruPatch.patchId(), aruPatch.version());
        this.aruPatch = aruPatch;
        this.userId = userId;
        this.password = password;

        if (Utils.isEmptyString(aruPatch.patchId())) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0058", aruPatch.patchId()));
        }
    }

    @Override
    public String getVersion() {
        return aruPatch.version();
    }

    void setVersion(String value) {
        aruPatch.version(value);
    }

    private boolean offlineMode() {
        return userId == null || password == null;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws IOException {
        String cacheKey = getKey();
        logger.entering(cacheKey);

        String filePath;
        boolean fileExists;

        filePath = cacheStore.getValueFromCache(cacheKey);
        fileExists = isFileOnDisk(filePath);

        if (fileExists) {
            logger.info("IMG-0017", getKey(), filePath);
        } else {
            logger.info("IMG-0061", getKey(), aruPatch.patchId());

            if (offlineMode()) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0056", getKey()));
            }
            filePath = downloadPatch(cacheStore);
        }

        logger.exiting(filePath);
        return filePath;
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException {
        String filename = AruUtil.rest().downloadAruPatch(aruPatch, cacheStore.getCacheDir(), userId, password);

        // after downloading the file, update the cache metadata
        String patchKey = getKey();
        logger.info("IMG-0060", patchKey, filename);
        cacheStore.addToCache(patchKey, filename);
        String filePath = cacheStore.getValueFromCache(patchKey);

        if (!isFileOnDisk(filePath)) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0037", aruPatch.patchId(), getVersion()));
        }

        return filePath;
    }

    @Override
    public String toString() {
        return getKey();
    }
}
