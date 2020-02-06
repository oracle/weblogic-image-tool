// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Utils;

public class CachedPatchFile extends CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CachedPatchFile.class);
    private String patchId;
    private String version;
    private String userId;
    private String password;

    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param version     the version of installer this patch is applicable to
     * @param patchId     the ID of the patch
     * @param userId      the username to use for retrieving the patch
     * @param password    the password to use with the userId to retrieve the patch
     */
    public CachedPatchFile(String version, String patchId, String userId, String password) {
        super(patchId, version);
        this.version = version;
        this.patchId = patchId;
        this.userId = userId;
        this.password = password;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws IOException {
        //patchId is null in case of latestPSU

        logger.entering(patchId);
        String filePath = cacheStore.getValueFromCache(getKey());
        boolean fileExists = isFileOnDisk(filePath);

        if (fileExists) {
            logger.info("IMG-0017", patchId, filePath);
        } else {
            logger.fine("Could not find patch in cache patchId={0} version={1} ", patchId, version);

            if (userId == null || password == null) {
                throw new FileNotFoundException(String.format(
                    "Patch %s is not in the cache store and you have not provide Oracle Support "
                        + "credentials in the command line.  Please provide --user with one of the password "
                        + "option or "
                        + "populate the cache store manually",
                    patchId));
            }
            logger.info("IMG-0018", patchId);
            filePath = downloadPatch(cacheStore);
        }

        logger.exiting(filePath);
        return filePath;
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException {
        // try downloading it
        List<String> patches = ARUUtil.getPatchesFor(Collections.singletonList(patchId),
                userId, password, cacheStore.getCacheDir());
        String patchKey = getKey();
        // we ignore the release number coming from ARUUtil patchId_releaseNumber=/path/to/patch.zip
        patches.forEach(x -> cacheStore.addToCache(patchKey, x.substring(x.indexOf('=') + 1)));
        String filePath = cacheStore.getValueFromCache(patchKey);
        if (!isFileOnDisk(filePath)) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0037", patchId, version));
        }
        return filePath;
    }
}
