// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class OPatchFile extends PatchFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(OPatchFile.class);

    public static final String DEFAULT_BUG_NUM = "28186730";

    /**
     * An abstract OPatch file to help resolve the local file, and determine local patch version.
     * Default patch number for OPatch is 28186730.
     *
     * @param patchId  the ID of the patch
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public OPatchFile(String patchId, String userId, String password, CacheStore cache)
        throws IOException, XPathExpressionException {
        //
        super(AruUtil.rest().getPatch(getDefaultBugNum(patchId), userId, password, "[access = 'Open access']"),
            userId,
            password);

        // when offline, use the local cache to determine newest version available for OPatch
        if (userId == null && password == null) {
            setVersion(getLatestCachedVersion(cache, getDefaultBugNum(patchId)));
        }
    }

    private static String getDefaultBugNum(String patchId) {
        if (patchId == null) {
            return DEFAULT_BUG_NUM;
        } else {
            return patchId;
        }
    }

    private static String getLatestCachedVersion(CacheStore cache, String patchId) {
        String latestVersion = "0.0.0.0.0";
        Set<String> keys = cache.getCacheItems().keySet();
        for (String key : keys) {
            if (key.startsWith(patchId)) {
                logger.fine("found OPatch entry in cache {0}", key);
                int split = key.indexOf('_');
                if (split < 0) {
                    continue;
                }
                String cacheVersion = key.substring(split + 1);
                if (Utils.compareVersions(latestVersion, cacheVersion) < 0) {
                    logger.fine("using cache {0} as newer OPatch version instead of {1}", key, latestVersion);
                    latestVersion = cacheVersion;
                }
            }
        }
        return latestVersion;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws IOException {
        try {
            return super.resolve(cacheStore);
        } catch (FileNotFoundException fnfe) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0062"));
        }
    }
}
