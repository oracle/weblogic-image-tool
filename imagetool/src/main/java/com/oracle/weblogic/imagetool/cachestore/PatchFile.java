// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;

public class PatchFile extends CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
    private static final String BUG_SEARCH_URL = "https://updates.oracle.com/Orion/Services/search?bug=%s";

    private final String bugNumber;
    private final boolean patchVersionProvided;
    private final String userId;
    private final String password;
    private String patchVersion;
    private String patchSetVersion;
    // initialized after call to ARU
    private Document aruInfo = null;
    // derived from aruInfo
    private String releaseNumber = null;
    private String releaseName = null;


    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param patchId  the ID of the patch
     * @param version  the version of installer this patch is applicable
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public PatchFile(String patchId, String version, String patchSetVersion, String userId, String password) {
        super(patchId, version);
        this.userId = userId;
        this.password = password;
        this.patchSetVersion = patchSetVersion;

        // if the user provided the version for the patch as xxx_yyy, separate xxx as bugnumber and yyy as patchVersion
        int ind = patchId.indexOf('_');
        if (ind > 0) {
            bugNumber = patchId.substring(0, ind);
            patchVersion = patchId.substring(ind + 1);
            patchVersionProvided = true;
        } else {
            bugNumber = patchId;
            patchVersion = null;
            patchVersionProvided = false;
        }

        if (Utils.isEmptyString(bugNumber)) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0058", patchId));
        }
    }

    @Override
    public String getVersion() {
        if (patchVersion != null) {
            return patchVersion;
        } else {
            return super.getVersion();
        }
    }

    private boolean offlineMode() {
        return userId == null || password == null;
    }

    private boolean checkPsuVersion() {
        return !patchVersionProvided && patchSetVersion != null;
    }

    @Override
    public String resolve(CacheStore cacheStore) throws IOException, XPathExpressionException {
        String cacheKey = getKey();
        logger.entering(cacheKey, patchSetVersion);
        String filePath;
        boolean fileExists;
        // check the cache for a PSU version of the patch, before checking the installer version of the patch
        if (checkPsuVersion()) {
            cacheKey = buildKeyFromVersion(patchSetVersion);
            logger.info("IMG-0074", bugNumber, cacheKey);
        }

        filePath = cacheStore.getValueFromCache(cacheKey);
        fileExists = isFileOnDisk(filePath);

        // if it was checking the PSU version, and could not be found locally ...
        if (!fileExists && checkPsuVersion()) {
            // if online, check ARU for a PSU version of the patch
            if (!offlineMode()) {
                // get ARU info to see if there are multiple patches for this bug number
                if (needAruInfo()) {
                    initPatchInfo();
                }
                String patchXpath = String.format("string(/results/patch/release[@name='%s'])", patchSetVersion);
                String patch = XPathUtil.applyXPathReturnString(getAruInfo(), patchXpath);
                // if the patch has a PSU version in ARU, set the patch version to PSU version (download PSU version)
                if (!Utils.isEmptyString(patch)) {
                    setPatchVersion(patchSetVersion);
                }
            } else {
                // if offline, use the default cache key to resolve patch locally
                cacheKey = getKey();
                filePath = cacheStore.getValueFromCache(cacheKey);
                fileExists = isFileOnDisk(filePath);
            }
        }

        if (fileExists) {
            logger.info("IMG-0017", getKey(), filePath);
        } else {
            logger.info("IMG-0061", getKey(), getBugNumber());

            if (offlineMode()) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0056", getKey()));
            }
            filePath = downloadPatch(cacheStore);
        }

        logger.exiting(filePath);
        return filePath;
    }

    /**
     * Get the bug number for this patch.
     *
     * @return the bug number.
     */
    public String getBugNumber() {
        return bugNumber;
    }

    Document getAruInfo() {
        return aruInfo;
    }

    void setPatchVersion(String value) {
        patchVersion = value;
    }

    boolean needAruInfo() {
        return aruInfo == null && !offlineMode();
    }

    boolean isPatchVersionProvided() {
        return patchVersionProvided;
    }

    /**
     * Populate internal patch metadata from Oracle ARU system.
     *
     * @throws IOException if an error occurs trying to get response or parsing response from ARU.
     */
    synchronized void initPatchInfo() throws IOException {
        logger.entering(bugNumber);
        if (needAruInfo()) {
            String url = String.format(BUG_SEARCH_URL, bugNumber);
            aruInfo = HttpUtil.getXMLContent(url, userId, password);
        }
        //confirmUniquePatchSelection();
        //if (checkPsuVersion()) {
        //    String patchXpath = String.format("string(/results/patch/release[@name='%s'])", patchSetVersion);
        //    String patch = XPathUtil.applyXPathReturnString(aruInfo, patchXpath);
        //    // if the patch has a PSU version in ARU, set the patch version to PSU version (download PSU version)
        //    if (!Utils.isEmptyString(patch)) {
        //        setPatchVersion(patchSetVersion);
        //    }
        //}

        logger.exiting();
    }

    /**
     * Get the release name for this patch.
     *
     * @return the release name provided by ARU
     * @throws IOException if an error occurs retrieving the release number/name from ARU
     */
    public String getReleaseName() throws IOException, XPathExpressionException {
        if (releaseNumber != null) {
            return releaseName;
        }
        getReleaseNumber();
        return releaseName;
    }

    /**
     * Get the internal release number for this patch.
     *
     * @return the release number provided by ARU
     * @throws IOException if an error occurs retrieving the release number from ARU
     */
    public String getReleaseNumber() throws IOException, XPathExpressionException {
        if (releaseNumber != null) {
            return releaseNumber;
        }

        if (needAruInfo()) {
            initPatchInfo();
        }

        String releasePath = String.format("/results/patch/release[@name='%s']/@id", getVersion());
        String releaseNamePath = String.format("/results/patch/release[@name='%s']/text()", getVersion());

        logger.finest("Searching for release number with xpath: {0}", releasePath);
        releaseNumber = XPathUtil.applyXPathReturnString(aruInfo, releasePath);
        releaseName = XPathUtil.applyXPathReturnString(aruInfo, releaseNamePath);

        if (Utils.isEmptyString(releaseNumber)) {
            String error = Utils.getMessage("IMG-0057", bugNumber);
            logger.severe(error);
            throw new IOException(error);
        }

        logger.finest("XPath return release number {0}", releaseNumber);
        return releaseNumber;
    }

    /**
     * Set the patch version to be downloaded based on response from Oracle Support.
     * If the patch is found, but the version does not match what was requested, set the version to match
     * the response from Oracle Support and WARN the user.  OPatch apply <b>may</b> fail during the build.
     */
    boolean verifyPatchVersion() throws IOException, XPathExpressionException {
        List<String> versionsForPatch =
            XPathUtil.applyXPathReturnList(getAruInfo(), "/results/patch/release/@name");
        logger.fine("versions for patch {0} are {1}", getBugNumber(), versionsForPatch);

        if (!versionsForPatch.contains(getVersion())) {
            logger.fine("Could not find patch version {0} for bug {1}", getVersion(), getBugNumber());
            if (versionsForPatch.size() == 1) {
                // patch version was not provided, but there is only one patch.  Version in ARU does not match
                // installer version, so warn the user.
                String newVersion = versionsForPatch.get(0);
                setPatchVersion(newVersion);
                logger.warning("IMG-0063", getBugNumber(), newVersion);
                return true;
            } else {
                // there were more than one version of the patch, but the desired version was not found
                IOException ioe = new IOException(Utils.getMessage("IMG-0034", bugNumber,
                    versionsForPatch.stream()
                        .map(s -> getBugNumber() + "_" + s)
                        .collect(Collectors.joining(", "))));
                logger.throwing(ioe);
                throw ioe;
            }
        }
        return false;
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException, XPathExpressionException {
        if (needAruInfo()) {
            initPatchInfo();
        }
        boolean versionNumberChanged = verifyPatchVersion();

        // if the patch version was modified (by verify), check to see if the "new" version is in the cache
        if (versionNumberChanged) {
            String filePath = cacheStore.getValueFromCache(getKey());
            if (isFileOnDisk(filePath)) {
                logger.info("IMG-0017", getKey(), filePath);
                // found patch in the cache using new patch version, no need to download
                return filePath;
            }
        }

        // select the patch based on the version requested, or default to the installer version, see getVersion()
        String patchXpath = String.format("string(/results/patch[release[@name='%s']]", getVersion());

        String downloadUrlXpath = patchXpath + "/files/file/download_url";
        String downLoadLink = XPathUtil.applyXPathReturnString(getAruInfo(),downloadUrlXpath + "/text())");
        String downLoadHost = XPathUtil.applyXPathReturnString(getAruInfo(),downloadUrlXpath + "/@host)");
        logger.finer("using download URL xpath = {0}, found link={1}, host={2}",
            downloadUrlXpath, downLoadLink, downLoadHost);

        int index = downLoadLink.indexOf("patch_file=");

        if (index < 0) {
            throw new IOException(Utils.getMessage("IMG-0059", getBugNumber()));
        }
        // download the remote patch file to the local cache patch directory
        String filename = cacheStore.getCacheDir() + File.separator
            + downLoadLink.substring(index + "patch_file=".length());
        logger.info("IMG-0018", getBugNumber());
        downloadFile(downLoadHost + downLoadLink, filename, userId, password);

        // after downloading the file, update the cache metadata
        String patchKey = getKey();
        logger.info("IMG-0060", patchKey, filename);
        cacheStore.addToCache(patchKey, filename);
        String filePath = cacheStore.getValueFromCache(patchKey);

        if (!isFileOnDisk(filePath)) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0037", getBugNumber(), getVersion()));
        }

        return filePath;
    }

    @Override
    public String toString() {
        return getKey();
    }
}
