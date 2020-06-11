// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;

public class OPatchFile extends PatchFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(OPatchFile.class);

    public static final String DEFAULT_BUG_NUM = "28186730";

    /**
     * Create an abstract file to hold the metadata for a patch file.
     * Default patch number for OPatch is 28186730.
     * Default version for OPatch is 13.9.4.0.0.
     *
     * @param patchId  the ID of the patch
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public OPatchFile(String patchId, String userId, String password, CacheStore cache) {
        super(getPatchId(patchId), latestVersion(cache, patchId, userId, password), null, userId, password);
    }

    private static String getPatchId(String patchId) {
        if (patchId == null) {
            return DEFAULT_BUG_NUM;
        } else {
            return patchId;
        }
    }

    /**
     * If a version is not part of the patchId, search the cache for the latest version of the OPatch patch.
     * If the provided patchId is not null (not default), just use the default version number.
     *
     * @param cache    cache store to search
     * @param userId   user credential
     * @param password user credential
     * @return the latest in the cache or the default version number
     */
    private static String latestVersion(CacheStore cache, String patchId, String userId, String password) {
        String latestVersion = "0.0.0.0.0";
        // if patch version was not provided, and user/pass was not supplied, find the newest version in cache
        if (userId == null && password == null) {
            Set<String> keys = cache.getCacheItems().keySet();
            for (String key : keys) {
                if (key.startsWith(getPatchId(patchId))) {
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

        }
        return latestVersion;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws IOException, XPathExpressionException {
        if (needAruInfo()) {
            initPatchInfo();
        }
        try {
            return super.resolve(cacheStore);
        } catch (FileNotFoundException fnfe) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0062"));
        }
    }

    @Override
    boolean verifyPatchVersion() throws XPathExpressionException {
        // if the user did not provide the patch version on the command line, use the latest version from Oracle Support
        if (!isPatchVersionProvided()) {
            // grab the latest version for OPatch from Oracle Support
            String latestVersion = XPathUtil.applyXPathReturnString(getAruInfo(),
                "string(/results/patch[access = 'Open access']/release/@name)");
            setPatchVersion(latestVersion);
            logger.fine("From ARU, setting OPatch patch version to {0}", latestVersion);
            return true;
        }
        return false;
    }
}
