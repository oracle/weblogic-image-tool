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
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class PatchFile extends CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);
    private static final String BUG_SEARCH_URL = "https://updates.oracle.com/Orion/Services/search?bug=%s";

    private final String bugNumber;
    private String patchVersion;
    private final boolean patchVersionProvided;
    private final String userId;
    private final String password;

    // initialized after call to ARU
    private Document aruInfo = null;
    // derived from aruInfo
    private String releaseNumber = null;


    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param patchId     the ID of the patch
     * @param version     the version of installer this patch is applicable
     * @param userId      the username to use for retrieving the patch
     * @param password    the password to use with the userId to retrieve the patch
     */
    public PatchFile(String patchId, String version, String userId, String password) {
        super(patchId, version);
        this.userId = userId;
        this.password = password;

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

    @Override
    public String resolve(CacheStore cacheStore) throws IOException {
        logger.entering(getKey());
        String filePath = cacheStore.getValueFromCache(getKey());
        boolean fileExists = isFileOnDisk(filePath);

        if (fileExists) {
            logger.info("IMG-0017", getKey(), filePath);
        } else {
            logger.info("IMG-0061", getKey(), getBugNumber());

            if (userId == null || password == null) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0056", getKey()));
            }
            filePath = downloadPatch(cacheStore);
        }

        logger.exiting(filePath);
        return filePath;
    }

    /**
     * Get the bug number for this patch.
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
        return aruInfo == null && userId != null && password != null;
    }

    boolean isPatchVersionProvided() {
        return patchVersionProvided;
    }

    /**
     * Populate internal patch metadata from Oracle ARU system.
     * @throws IOException if an error occurs trying to get response or parsing response from ARU.
     */
    synchronized void initPatchInfo() throws IOException {
        logger.entering(getKey());
        if (needAruInfo()) {
            String url = String.format(BUG_SEARCH_URL, bugNumber);
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
     * When the bug number is provided without a patch version, verify that the bug has only one patch file.
     * @throws IOException if patch selection is non-unique, or if unable to read ARU metadata
     */
    void confirmUniquePatchSelection() throws IOException {
        if (isPatchVersionProvided()) {
            // patch version was specified, no need to check if bug number resolves to a single patch
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
        logger.info("IMG-0018", getBugNumber());

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
                throw new IOException(Utils.getMessage("IMG-0059", getBugNumber()));
            }
            // download the remote patch file to the local cache patch directory
            String filename = cacheStore.getCacheDir() + File.separator
                + downLoadLink.substring(index + "patch_file=".length());
            HttpUtil.downloadFile(downLoadHost + downLoadLink, filename, userId, password);

            // after downloading the file, update the cache metadata
            String patchKey = getKey();
            logger.info("IMG-0060", patchKey, filename);
            cacheStore.addToCache(patchKey, filename);
            String filePath = cacheStore.getValueFromCache(patchKey);

            if (!isFileOnDisk(filePath)) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0037", getBugNumber(), getVersion()));
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
