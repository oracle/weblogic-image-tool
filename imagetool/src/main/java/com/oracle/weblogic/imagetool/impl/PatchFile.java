// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.AbstractFile;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Utils;

public class PatchFile extends AbstractFile {

    private String patchId;
    private String category;
    private String version;
    private final Logger logger = Logger.getLogger(PatchFile.class.getName());

    /**
     * Create an abstract file to hold the metadata for a patch file.
     * @param cachePolicy the policy to apply.=
     * @param category the patch category
     * @param version the version of installer this patch is applicable to
     * @param patchId the ID of the patch
     * @param userId the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public PatchFile(CachePolicy cachePolicy, String category, String version, String patchId, String userId,
                     String password) {
        super(patchId, version, cachePolicy, userId, password);
        this.category = category;
        this.version = version;
        this.patchId = patchId;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        //patchId is null in case of latestPSU
        if (Utils.isEmptyString(patchId)) {
            if (cachePolicy == CachePolicy.ALWAYS) {
                throw new Exception("CachePolicy prohibits download. Cannot determine latestPSU");
            } else {
                patchId = ARUUtil.getLatestPSUNumber(category, version, userId, password);
                if (Utils.isEmptyString(patchId)) {
                    throw new Exception(String.format("Failed to find latest psu for product category %s, version %s",
                            category, version));
                }
            }
        }
        String filePath = cacheStore.getValueFromCache(getKey());
        boolean fileExists = isFileOnDisk(filePath);
        switch (cachePolicy) {
            case ALWAYS:
                if (!fileExists) {
                    throw new Exception(String.format(
                            "CachePolicy prohibits download. Download required patch %s for version %s and add it to "
                                    + "cache %s_%s=/path/to/patch.zip",
                            patchId, version, patchId, version));
                }
                break;
            case NEVER:
                filePath = downloadPatch(cacheStore);
                break;
            case FIRST:
            default:
                if (!fileExists) {
                    filePath = downloadPatch(cacheStore);
                }
        }
        return filePath;
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException {
        // try downloading it
        List<String> patches = ARUUtil.getPatchesFor(category, version, Collections.singletonList(patchId),
                userId, password, cacheStore.getCacheDir());
        String patchKey = getKey();
        // we ignore the release number coming from ARUUtil patchId_releaseNumber=/path/to/patch.zip
        patches.forEach(x -> cacheStore.addToCache(patchKey, x.substring(x.indexOf('=') + 1)));
        String filePath = cacheStore.getValueFromCache(patchKey);
        if (!isFileOnDisk(filePath)) {
            throw new IOException(String.format("Failed to find patch %s for product category %s, version %s",
                    patchId, category, version));
        }
        return filePath;
    }
}
