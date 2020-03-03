// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class PatchFile extends CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
    private String patchId;
    private String version;
    private String userId;
    private String password;
    private String bugNumber;
    private String patchVersion;

    // initialized after call to ARU
    private Document aruInfo = null;
    // derived from aruInfo
    private String releaseNumber = null;


    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param patchId     the ID of the patch
     * @param version     the version of installer this patch is applicable to
     * @param userId      the username to use for retrieving the patch
     * @param password    the password to use with the userId to retrieve the patch
     */
    public PatchFile(String version, String patchId, String userId, String password) {
        super(patchId, version);
        this.version = version;
        this.patchId = patchId;
        this.userId = userId;
        this.password = password;

        // if the user provided the version for the patch as xxx_yyy, separate xxx as bugnumber and yyy as patchVersion
        int ind = patchId.indexOf('_');
        if (ind > 0) {
            bugNumber = patchId.substring(0, ind);
            patchVersion = patchId.substring(ind + 1);
        } else {
            bugNumber = patchId;
            patchVersion = null;
        }

        if (Utils.isEmptyString(bugNumber)) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0058", patchId));
        }
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
                throw new FileNotFoundException(Utils.getMessage("IMG-0056", patchId));
            }
            filePath = downloadPatch(cacheStore);
        }

        logger.exiting(filePath);
        return filePath;
    }

    /**
     * Get the patch ID for this patch provided by the user input.
     * @return the patch ID provided by user input.
     */
    public String getPatchId() {
        return patchId;
    }

    public String getBugNumber() {
        return bugNumber;
    }

    private boolean needAruInfo() {
        return aruInfo == null;
    }

    private synchronized void initPatchInfo() throws IOException {
        logger.entering(patchId);
        if (needAruInfo()) {
            String url = String.format(Constants.BUG_SEARCH_URL, bugNumber);
            aruInfo = HttpUtil.getXMLContent(url, userId, password);
        }
        confirmUniquePatchSelection();
        logger.exiting();
    }

    /**
     * Get the internal release number for this patch.
     * @return the release number provided by ARU
     * @throws IOException if an error occurs retrieving the release number from ARU
     */
    public String getReleaseNumber() throws IOException {
        if (releaseNumber != null) {
            return releaseNumber;
        }

        if (needAruInfo()) {
            initPatchInfo();
        }

        String xpath;
        if (patchVersion != null) {
            xpath = String.format("/results/patch/release[@name='%s']/@id", patchVersion);
        } else {
            xpath = "/results/patch/release/@id";
        }

        logger.finest("Searching for release number with xpath: {0}", xpath);
        try {
            releaseNumber = XPathUtil.applyXPathReturnString(aruInfo, xpath);

            if (Utils.isEmptyString(releaseNumber)) {
                String error = Utils.getMessage("IMG-0057", bugNumber);
                logger.severe(error);
                throw new IOException(error);
            }

            logger.finest("XPath return release number {0}", releaseNumber);
            return releaseNumber;
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    /**
     * Validate the patch selection criteria (bug number and patch version) will resolve to a unique patch.
     * @throws IOException if patch selection is non-unique, or if unable to read ARU metadata
     */
    private void confirmUniquePatchSelection() throws IOException {
        if (patchVersion != null) {
            // patch version was specified, no need to check
            return;
        }

        if (needAruInfo()) {
            initPatchInfo();
        }

        // if only the base bug number was provided, verify that there is only one patch for that bug number
        try {
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(aruInfo, "/results/patch");
            if (nodeList.getLength() > 1) {
                // ERROR - found more than one patch for bug number
                List<String> patchVersions = new ArrayList<>();
                for (int i = 1; i <= nodeList.getLength(); i++) {
                    String xpath = String.format("string(/results/patch[%d]/release/@name)", i);
                    String patchVer = XPathUtil.applyXPathReturnString(aruInfo, xpath);
                    patchVersions.add(bugNumber + "_" + patchVer);
                }

                IOException ioe = new IOException(Utils.getMessage("IMG-0034", bugNumber,
                    String.join(", ", patchVersions)));
                logger.throwing(ioe);
                throw ioe;
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    private String downloadPatch(CacheStore cacheStore) throws IOException {
        logger.info("IMG-0018", patchId);

        if (needAruInfo()) {
            initPatchInfo();
        }
        try {
            Document patchMetadata = aruInfo;
            // if the patchVersion is specified, narrow the list of patches with the value of patchVersion
            if (patchVersion != null) {
                String xpath = String.format("/results/patch[release[@name='%s']]", patchVersion);
                NodeList matchedResult = XPathUtil.applyXPathReturnNodeList(aruInfo, xpath);
                patchMetadata = ARUUtil.createResultDocument(matchedResult);
                logger.finest(XPathUtil.prettyPrint(patchMetadata));
            }

            String downLoadLink = XPathUtil.applyXPathReturnString(patchMetadata, "string"
                + "(/results/patch[1]/files/file/download_url/text())");

            String downLoadHost = XPathUtil.applyXPathReturnString(patchMetadata, "string"
                + "(/results/patch[1]/files/file/download_url/@host)");

            int index = downLoadLink.indexOf("patch_file=");

            if (index < 0) {
                throw new IOException(Utils.getMessage("IMG-0059", patchId));
            }
            // download the remote patch file to the local cache patch directory
            String filename = cacheStore.getCacheDir() + File.separator
                + downLoadLink.substring(index + "patch_file=".length());
            HttpUtil.downloadFile(downLoadHost + downLoadLink, filename, userId, password);

            // after downloading the file, update the cache metadata
            String patchKey = getKey();
            logger.fine("Adding patch {0} to cache: {1}", patchKey, filename);
            cacheStore.addToCache(patchKey, filename);
            String filePath = cacheStore.getValueFromCache(patchKey);

            if (!isFileOnDisk(filePath)) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0037", patchId, version));
            }

            return filePath;
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    @Override
    public String toString() {
        return getKey();
    }
}
