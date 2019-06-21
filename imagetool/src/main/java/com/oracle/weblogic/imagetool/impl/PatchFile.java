/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.impl;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.AbstractFile;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class PatchFile extends AbstractFile {

    private String patchId;
    private String category;
    private String version;
    private final Logger logger = Logger.getLogger(PatchFile.class.getName());

    public PatchFile(CachePolicy cachePolicy, String category, String version, String patchId, String userId, String password) {
        super(patchId, version, cachePolicy, userId, password);
        this.category = category;
        this.version = version;
        this.patchId = patchId;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        //patchId is null in case of latestPSU

        String filePath = cacheStore.getValueFromCache(getKey());
        boolean fileExists = isFileOnDisk(filePath);


        logger.finest("PatchFile.resolve: Patch file in cache?: " + fileExists);

        if (!fileExists) {
            if (userId == null || password == null)
                throw new Exception(String.format(
                    "Patch %s is not in the cache store and you do not provide Oracle Support "
                        + "credentials.  Please provide --user with one of the password option or "
                        + "populate the cache store manually",
                    patchId));
            filePath = downloadPatch(cacheStore);
        }

//        switch (cachePolicy) {
//            case ALWAYS:
//                if (!fileExists) {
//                    if (userId == null || password == null)
//                        throw new Exception(String.format(
//                                "Patch %s is not in the cache store and you have not provide Oracle Support "
//                                    + "credentials.  Please provide --user with one of the password option or "
//                                    + "populate the cache store manually",
//                                patchId));
//                    filePath = downloadPatch(cacheStore);
//                }
//                break;
//        }
        logger.finest("PatchFile.resolve: resolved filepath " + filePath);
        return filePath;
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException {
        // try downloading it
        List<String> patches = ARUUtil.getPatchesFor( Collections.singletonList(patchId),
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
