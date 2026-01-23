// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.NoPatchesFoundException;
import com.oracle.weblogic.imagetool.aru.VersionNotFoundException;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class OPatchFile extends PatchFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(OPatchFile.class);

    /**
     * Patch ID for OPatch 13.9.4.x.x. OPatch for Java EE based products (javax).
      */
    private static final String PATCH1394 = "28186730";
    /**
     * Patch ID for OPatch 13.9.6.x.x. OPatch for Jakarta EE based products.
     */
    private static final String PATCH1396 = "38256237";

    /**
     * Determines the OPatch bug number (patchid) based on the provided product version.
     * @param installerVersion the version of the installed product such as WebLogic Server.
     * @return the bug ID for OPatch
     */
    public static String getOpatchBugNum(String installerVersion) {
        // Versions newer than 14.1.2 are Jakarta-based
        if (Utils.compareVersions("14.1.2.25", installerVersion) > 0) {
            return PATCH1394;
        } else {
            return PATCH1396;
        }
    }

    /**
     * Create an abstract OPatch file to help resolve the local file or download the remote patch.
     *
     * @param patchId  bug number and optional version
     * @param installerVersion  version of product to be patched
     * @param userid   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     * @param cache    the local cache used for patch storage
     * @return an abstract OPatch file
     */
    public static OPatchFile getInstance(String patchId, String installerVersion,
                                         String userid, String password, CacheStore cache)
        throws AruException, XPathExpressionException, IOException {

        logger.entering(patchId);
        String patchNumber = patchId;
        String providedVersion = null;
        if (patchId == null) {
            // if the user did not provide a patch number, assume the default OPatch bug number
            patchNumber = getOpatchBugNum(installerVersion);
        } else if (patchId.contains(CacheStore.CACHE_KEY_SEPARATOR)) {
            // if the user provides a specific version, use that version or fail.  like 28186730_13.9.4.2.4
            int separator = patchId.indexOf(CacheStore.CACHE_KEY_SEPARATOR);
            patchNumber = patchId.substring(0, separator);
            providedVersion = patchId.substring(separator + 1);
            logger.fine("User provided OPatch version {0} {1}", patchNumber, providedVersion);
        }

        AruPatch selectedPatch;
        if (isOffline(userid, password)) {
            selectedPatch = getAruPatchOffline(patchNumber, providedVersion, cache);
        } else {
            try {
                selectedPatch = getAruPatchOnline(patchNumber, providedVersion, userid, password);
            } catch (VersionNotFoundException notFound) {
                // Could not find the user requested OPatch version on ARU, checking local cache before giving up
                if (cache.containsKey(patchId)) {
                    logger.info("OPatch version {0} is not available online, using cached copy.", providedVersion);
                    selectedPatch = new AruPatch().patchId(patchNumber).version(providedVersion);
                } else {
                    logger.severe("IMG-0101", providedVersion);
                    throw notFound;
                }
            }
        }

        logger.exiting(selectedPatch);
        return new OPatchFile(selectedPatch, userid, password);
    }

    private static boolean isOffline(String userid, String password) {
        return userid == null || password == null;
    }

    private static AruPatch getAruPatchOffline(String patchNumber, String providedVersion, CacheStore cache) {
        // if working offline, update the placeholder in the list to have the provided version or the latest cached
        AruPatch offlinePatch = new AruPatch().patchId(patchNumber);
        if (Utils.isEmptyString(providedVersion)) {
            // user did not request a specific version, find the latest version of OPatch in the cache
            offlinePatch.version(getLatestCachedVersion(cache, patchNumber));
        } else {
            offlinePatch.version(providedVersion);
        }

        return offlinePatch;
    }

    private static AruPatch getAruPatchOnline(String patchNumber, String providedVersion,
                                              String userid, String password)
        throws XPathExpressionException, IOException, AruException {

        List<AruPatch> patches = AruUtil.rest().getPatches(patchNumber, userid, password)
            .filter(p -> p.isApplicableToTarget(2000)) // OPatch is always platform 2000/generic
            .filter(AruPatch::isOpenAccess) // filter ARU results based on access flag (discard protected versions)
            .collect(Collectors.toList());
        logger.fine("Found {0} OPatch versions for id {1}", patches.size(), patchNumber);

        AruPatch selectedPatch;
        if (patches.isEmpty()) {
            throw new NoPatchesFoundException(Utils.getMessage("IMG-0057", patchNumber));
        } else if (providedVersion != null) {
            selectedPatch = patches.stream()
                .filter(p -> providedVersion.equals(p.version())).findAny().orElse(null);
            if (selectedPatch == null) {
                throw new VersionNotFoundException(patchNumber, providedVersion, patches);
            }
        } else {
            // Compare the ARU OPatch patches using the patch version field, like 12.2.1.4.0
            Comparator<AruPatch> patchVersionComparator =
                Comparator.comparing(AruPatch::version, Utils::compareVersionsNullsFirst);
            // Select the newest (highest version) OPatch install/patch
            selectedPatch = patches.stream().max(patchVersionComparator).orElse(null);
        }
        return selectedPatch;
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
        if (PATCH1394.equals(patchId) || PATCH1396.equals(patchId)) {
            return true;
        }
        return patchId != null
            && (patchId.startsWith(PATCH1394 + CacheStore.CACHE_KEY_SEPARATOR)
              || patchId.startsWith(PATCH1396 + CacheStore.CACHE_KEY_SEPARATOR));
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
