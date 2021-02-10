// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class OPatchFile extends PatchFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(OPatchFile.class);

    public static final String DEFAULT_BUG_NUM = "28186730";

    /**
     * Create an abstract OPatch file to help resolve the local file or download the remote patch.
     *
     * @param patchId  bug number and optional version
     * @param userid   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     * @param cache    the local cache used for patch storage
     * @return an abstract OPatch file
     */
    public static OPatchFile getInstance(String patchId, String userid, String password, CacheStore cache)
        throws AruException, XPathExpressionException, IOException {

        logger.entering(patchId);
        String patchNumber = patchId;
        String providedVersion = null;
        if (patchId == null) {
            // if the user did not provide a patch number, assume the default OPatch bug number
            patchNumber = DEFAULT_BUG_NUM;
        } else if (patchId.contains(CacheStore.CACHE_KEY_SEPARATOR)) {
            // if the user provides a specific version, use that version or fail.  like 28186730_13.9.4.2.4
            int separator = patchId.indexOf(CacheStore.CACHE_KEY_SEPARATOR);
            patchNumber = patchId.substring(0, separator);
            providedVersion = patchId.substring(separator + 1);
            logger.fine("User provided OPatch version {0} {1}", patchNumber, providedVersion);
        }

        List<AruPatch> patches = AruUtil.rest().getPatches(patchNumber, userid, password);
        if (!isOffline(userid, password)) {
            // if working online with ARU metadata, filter results based on availability and ARU recommendation
            Stream<AruPatch> filteredList = patches.stream().filter(AruPatch::isOpenAccess);
            if (providedVersion == null) {
                // When the user did not provide a specific version, try to reduce the patch list to one patch.
                // There should only be ONE recommended patch/install for OPatch
                filteredList = filteredList.filter(AruPatch::isRecommended);
            }
            patches = filteredList.collect(Collectors.toList());
        }

        // For the OPatch use case, only the provided version (--opatchBugNumber) is used to "select" a patch.
        // selectPatch will return null if there is more than one patch in the patches list.
        AruPatch selectedPatch = AruPatch.selectPatch(patches, providedVersion, null, null);

        if (selectedPatch != null) {
            // if offline, find the latest OPatch version available in the cache
            if (isOffline(userid, password) && providedVersion == null) {
                selectedPatch.version(getLatestCachedVersion(cache, patchNumber));
            }
        } else {
            // select first recommended patch
            List<AruPatch> sortedList = patches.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            selectedPatch = sortedList.get(0);
            logger.warning("IMG-0057", selectedPatch.version(), sortedList.stream()
                .map(s -> s.patchId() + "_" + s.version())
                .collect(Collectors.joining(", ")));
        }
        logger.exiting(selectedPatch);
        return new OPatchFile(selectedPatch, userid, password);
    }

    private static boolean isOffline(String userid, String password) {
        return userid == null || password == null;
    }

    /**
     * An abstract OPatch file to help resolve the local file, and determine local patch version.
     * Default patch number for OPatch is 28186730.
     *
     * @param patch    the OPatch ARU patch
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    private OPatchFile(AruPatch patch, String userId, String password) {
        super(patch, userId, password);
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

    /**
     * Return true if the patchId matches any known OPatch bug numbers.
     *
     * @param patchId the patch ID to test
     * @return true if and only if the patchId matches an OPatch bug number.
     */
    public static boolean isOPatchPatch(String patchId) {
        if (DEFAULT_BUG_NUM.equals(patchId)) {
            return true;
        }
        return patchId != null && patchId.startsWith(DEFAULT_BUG_NUM + CacheStore.CACHE_KEY_SEPARATOR);
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
