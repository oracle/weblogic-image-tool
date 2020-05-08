// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.PatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ARUUtil {

    static final Map<String, String> releaseNumbersMap = new HashMap<>();
    private static final LoggingFacade logger = LoggingFactory.getLogger(ARUUtil.class);

    private ARUUtil() {
        // hide constructor, usage of this class is only static utilities
    }

    /**
     * Get list of PSU available for given category and release.
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static String getLatestPSUNumber(FmwInstallerType category, String version, String userId, String password)
        throws Exception {
        return getLatestPSUNumber(new SearchHelper(category, version, userId, password));
    }

    /**
     * Get the Latest PSU patch number for the search information in the helper.
     *
     * @param searchHelper containing category, version, credentials for the search
     * @return PSU patch number
     * @throws Exception if the search or xml parse of result fails
     */
    public static String getLatestPSUNumber(SearchHelper searchHelper) throws Exception {
        logger.entering(searchHelper);
        try {
            logger.info("IMG-0019");
            searchHelper.setRelease(getReleaseNumber(searchHelper));
            getRecommendedPsuMetadata(searchHelper);
            if (searchHelper.isSuccess()) {
                Document results = searchHelper.getResults();
                String result = XPathUtil.applyXPathReturnString(results, "/results/patch[1]/name");
                logger.exiting(result);
                logger.info("IMG-0020", result);
                return result;
            } else if (!Utils.isEmptyString(searchHelper.getErrorMessage())) {
                logger.warning("IMG-0023", searchHelper.getCategory(), searchHelper.getVersion());
                logger.fine(searchHelper.getErrorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0032", 
                    searchHelper.getCategory(), searchHelper.getVersion()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0032", 
                searchHelper.getCategory(), searchHelper.getVersion()), e);
        }
        logger.exiting();
        return null;
    }

    /**
     * Get list of PSU available for given category and release.
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static List<String> getLatestPSURecommendedPatches(FmwInstallerType category, String version,
                                                        String userId, String password) throws Exception {
        return getLatestPSURecommendedPatches(new SearchHelper(category, version, userId, password));
    }

    /**
     * Get the latest PSU along with recommended patches for the information in the search helper.
     *
     * @param searchHelper containing the search category, version and credentials
     * @return List of recommended patches
     * @throws Exception the search or resulting parse failed
     */
    public static List<String> getLatestPSURecommendedPatches(SearchHelper searchHelper)
        throws Exception {
        logger.entering(searchHelper);
        try {
            logger.info("IMG-0067");
            searchHelper.setRelease(getReleaseNumber(searchHelper));
            getRecommendedPatchesMetadata(searchHelper);
            if (searchHelper.isSuccess()) {
                Document results = searchHelper.getResults();
                NodeList nodeList = XPathUtil.applyXPathReturnNodeList(results, "/results/patch");
                List<String> result = new ArrayList<>();
                for (int i = 1; i <= nodeList.getLength(); i++) {
                    String patchId = XPathUtil.applyXPathReturnString(results,
                        String.format("string(/results/patch[%d]/name)", i));
                    String patchVersion = XPathUtil.applyXPathReturnString(results,
                        String.format("string(/results/patch[%d]/release/@name)", i));
                    if (!Utils.isEmptyString(patchVersion)) {
                        patchId = patchId + '_' + patchVersion;
                    }
                    logger.info("IMG-0068", patchId);
                    result.add(patchId);
                }
                logger.exiting(result);
                return result;
            } else if (!Utils.isEmptyString(searchHelper.getErrorMessage())) {
                logger.warning("IMG-0069", searchHelper.getCategory(), searchHelper.getVersion());
                logger.fine(searchHelper.getErrorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0070", 
                    searchHelper.getCategory(), searchHelper.getVersion()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0070", 
                searchHelper.getCategory(), searchHelper.getVersion()), e);
        }
        logger.exiting();
        return null;
    }

    /**
     * Validate patches conflicts by passing a list of patches.
     *
     * @param inventoryContent opatch lsinventory content (null if non is passed)
     * @param patches          A list of patches number
     * @param userId           userid for support account
     * @param password         password for support account
     * @throws IOException when failed to access the aru api
     */

    public static void validatePatches(String inventoryContent, List<PatchFile> patches, String userId, String password)
        throws IOException {

        logger.entering(inventoryContent, patches, userId);

        ValidationResult validationResult = new ValidationResult();
        validationResult.setSuccess(true);
        validationResult.setResults(null);
        if (userId == null || password == null) {
            logger.warning(Utils.getMessage("IMG-0033"));
            return;
        }
        logger.info("IMG-0012");

        StringBuilder payload = new StringBuilder("<conflict_check_request><platform>2000</platform>");

        if (inventoryContent != null) {
            String upiPayload = "<inventory_upi_request><lsinventory_output>" + inventoryContent
                    + "</lsinventory_output></inventory_upi_request>";

            Document upiResult = HttpUtil.postCheckConflictRequest(Constants.GET_LSINVENTORY_URL, upiPayload, userId,
                    password);

            try {
                NodeList upiList = XPathUtil.applyXPathReturnNodeList(upiResult,
                        "/inventory_upi_response/upi");
                if (upiList.getLength() > 0) {
                    payload.append("<target_patch_list>");

                    for (int ii = 0; ii < upiList.getLength(); ii++) {
                        Node upi = upiList.item(ii);
                        NamedNodeMap m = upi.getAttributes();
                        payload.append(String.format("<installed_patch upi=\"%s\"/>",
                                m.getNamedItem("number").getNodeValue()));

                    }
                    payload.append("</target_patch_list>");
                } else {
                    payload.append("<target_patch_list/>");
                }
            } catch (XPathExpressionException xpe) {
                throw new IOException(xpe);

            }

        } else {
            payload.append("<target_patch_list/>");
        }

        if (patches != null && !patches.isEmpty()) {
            payload.append("<candidate_patch_list>");
            // add the list of patches to the payload for ARU patch conflict check
            for (PatchFile patch : patches) {
                if (patch == null) {
                    logger.finer("Skipping null patch");
                    continue;
                }

                logger.info("IMG-0022", patch.getBugNumber(), patch.getReleaseNumber());

                payload.append(String.format("<patch_group rel_id=\"%s\">%s</patch_group>",
                        patch.getReleaseNumber(), patch.getBugNumber()));
            }
            payload.append("</candidate_patch_list>");
        }
        payload.append("</conflict_check_request>");

        logger.fine("Posting to ARU conflict check");
        Document result = HttpUtil.postCheckConflictRequest(Constants.CONFLICTCHECKER_URL, payload.toString(), userId,
                password);
        try {
            NodeList conflictSets = XPathUtil.applyXPathReturnNodeList(result, "/conflict_check/conflict_sets/set");
            if (conflictSets.getLength() > 0) {
                try {
                    validationResult.setSuccess(false);
                    String expression = "/conflict_check/conflict_sets/set/merge_patches";

                    NodeList nodeList = XPathUtil.applyXPathReturnNodeList(result, expression);

                    Document doc = createResultDocument(nodeList);

                    validationResult.setResults(doc);
                    validationResult.setErrorMessage(parsePatchValidationError(doc));

                } catch (XPathExpressionException xpe) {
                    throw new IOException(xpe);
                }

            }

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);

        }
        if (validationResult.isSuccess()) {
            logger.info("IMG-0006");
        } else {
            String error = validationResult.getErrorMessage();
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }
        logger.exiting(validationResult);
    }

    private static Document getAllReleases(SearchHelper searchHelper) throws IOException {
        logger.entering(searchHelper);
        searchHelper.getXmlContent(Constants.REL_URL);

        try {

            String expression;

            if (FmwInstallerType.WLS == searchHelper.getCategory()) {
                expression = "/results/release[starts-with(text(), 'Oracle WebLogic Server')]";
            } else if (Constants.OPATCH_PATCH_TYPE.equalsIgnoreCase(searchHelper.getCategory().toString())) {
                expression = "/results/release[starts-with(text(), 'OPatch')]";
            } else {
                expression = "/results/release[starts-with(text(), 'Fusion Middleware Upgrade')]";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(searchHelper.getResults(), expression);
            Document doc = createResultDocument(nodeList);
            logger.exiting();
            return doc;

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    private static void getRecommendedPsuMetadata(SearchHelper searchHelper) throws IOException {

        logger.entering();
        String productId = FmwInstallerType.WLS
            == searchHelper.getCategory() ? Constants.WLS_PROD_ID : Constants.FMW_PROD_ID;
        String url = String.format(Constants.RECOMMENDED_PATCHES_URL, productId, searchHelper.getRelease())
            + Constants.ONLY_GET_RECOMMENDED_PSU;
        logger.finer("getting PSU info from {0}", url);

        searchHelper.getXmlContent(url);
        logger.exiting();
    }

    private static void getRecommendedPatchesMetadata(SearchHelper searchHelper) throws IOException {

        logger.entering();
        String productId = FmwInstallerType.WLS
            == searchHelper.getCategory() ? Constants.WLS_PROD_ID : Constants.FMW_PROD_ID;
        String url = String.format(Constants.RECOMMENDED_PATCHES_URL, productId, searchHelper.getRelease());
        logger.finer("getting recommended patches info from {0}", url);

        searchHelper.getXmlContent(url);
        logger.exiting();
    }

    /**
     * Given a product category (wls, fmw, opatch) and version, determines the release number corresponding to that
     * in the ARU database.
     *
     * @param searchHelper helper with information for release search
     * @return release number
     * @throws IOException in case of error
     */
    private static String getReleaseNumber(SearchHelper searchHelper)
        throws IOException {
        logger.entering(searchHelper);
        String key = searchHelper.getCategory() + CacheStore.CACHE_KEY_SEPARATOR + searchHelper.getVersion();
        String retVal = releaseNumbersMap.getOrDefault(key, null);
        if (Utils.isEmptyString(retVal)) {
            logger.fine("Retrieving product release numbers from Oracle...");
            Document allReleases = getAllReleases(searchHelper);

            String expression = String.format("string(/results/release[@name = '%s']/@id)", searchHelper.getVersion());
            try {
                retVal = XPathUtil.applyXPathReturnString(allReleases, expression);
                logger.fine("Release number for {0} is {1}", searchHelper.getCategory(), retVal);
            } catch (XPathExpressionException xpe) {
                throw new IOException(xpe);
            }
            if (!Utils.isEmptyString(retVal)) {
                releaseNumbersMap.put(searchHelper.getCategory()
                    + CacheStore.CACHE_KEY_SEPARATOR + searchHelper.getVersion(), retVal);
            } else {
                throw new IOException(String.format("Failed to determine release number for category %s, version %s",
                        searchHelper.getCategory(), searchHelper.getVersion()));
            }
        }
        logger.exiting(retVal);
        return retVal;
    }

    /**
     * Validates whether the given username and password are valid MOS credentials.
     *
     * @param username support email id
     * @param password password
     * @return true if credentials are valid
     */
    public static boolean checkCredentials(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        SearchHelper searchHelper = new SearchHelper(null, null, username, password);
        try {
            searchHelper.getXmlContent(Constants.ARU_LANG_URL);
        } catch (IOException e) {
            Throwable cause = (e.getCause() == null) ? e : e.getCause();
            if (cause.getClass().isAssignableFrom(HttpResponseException.class)
                    && ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
        }
        return searchHelper.isSuccess();
    }

    /**
     * Parses the patch conflict check result and returns a string of patch conflicts grouped by each conflict.
     *
     * @param conflictsResultNode xml node representing the conflict check result
     * @return String
     */
    private static String parsePatchValidationError(Node conflictsResultNode) {
        StringBuilder stringBuilder = new StringBuilder();
        if (conflictsResultNode != null) {
            try {
                NodeList patchSets = XPathUtil.applyXPathReturnNodeList(conflictsResultNode, "//merge_patches");
                stringBuilder.append("patch conflicts detected: ");
                for (int i = 0; i < patchSets.getLength(); i++) {
                    stringBuilder.append("[");
                    NodeList bugNumbers = XPathUtil.applyXPathReturnNodeList(patchSets.item(i), "patch/bug/number"
                            + "/text()");
                    for (int j = 0; j < bugNumbers.getLength(); j++) {
                        stringBuilder.append(bugNumbers.item(j).getNodeValue());
                        stringBuilder.append(",");
                    }
                    if (bugNumbers.getLength() > 0) {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                    stringBuilder.append("]");
                }
            } catch (XPathExpressionException e) {
                return "Exception occurred in parsePatchValidationError: " + e.getMessage();
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Create an XML document from an XML NodeList.
     * @param nodeList partial XML document
     * @return a Document based on the NodeList provided
     * @throws IOException if an XML parser exception occurs trying to build the document
     */
    private static Document createResultDocument(NodeList nodeList) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            // Prevent XXE attacks
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element element = doc.createElement("results");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i);
                Node copyNode = doc.importNode(n, true);

                if (n instanceof Element) {
                    element.appendChild(copyNode);
                }
            }

            doc.appendChild(element);

            return doc;
        } catch (ParserConfigurationException pce) {
            throw new IOException(pce);
        }

    }
}

