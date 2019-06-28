// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ARUUtil {

    private static final Map<String, String> releaseNumbersMap = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ARUUtil.class.getName());

    /**
     * Return All WLS releases information.
     *
     * @param userId   userid for support account
     * @param password password for support account
     * @throws IOException when failed to access the aru api
     */

    public static Document getAllWLSReleases(String userId, String password) throws IOException {
        return getAllReleases("wls", userId, password);
    }

    /**
     * Return release number of a WLS release by version.
     *
     * @param version  wls version 12.2.1.3.0 etc ...
     * @param userId   user id for support account
     * @param password password for support account
     * @return release number or empty string if not found
     * @throws IOException when failed to access the aru api
     */
    private static String getWLSReleaseNumber(String version, String userId, String password) throws
            IOException {
        return getReleaseNumber("wls", version, userId, password);
    }

    /**
     * Return release number of a FMW release by version.
     *
     * @param version  wls version 12.2.1.3.0 etc ...
     * @param userId   user id for support account
     * @param password password for support account
     * @return release number or empty string if not found
     * @throws IOException when failed to access the aru api
     */
    private static String getFMWReleaseNumber(String version, String userId, String password) throws
            IOException {
        return getReleaseNumber("fmw", version, userId, password);
    }


    /**
     * Return All FMW releases information.
     *
     * @param userId   userid for support account
     * @param password password for support account
     * @throws IOException when failed to access the aru api
     */
    public static void getAllFMWReleases(String userId, String password) throws IOException {
        getAllReleases("fmw", userId, password);
    }


    /**
     * Download the latest PSU for given category and release.
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @param password password
     * @return bug number
     * @throws IOException when failed
     */
    public static String getLatestPSUFor(String category, String version, String userId, String password,
                                         String destDir) throws IOException {
        String releaseNumber = getReleaseNumber(category, version, userId, password);
        return getLatestPSU(category, releaseNumber, userId, password, destDir);
    }

    /**
     * Get list of PSU available for given category and release.
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     * @throws IOException when failed
     */
    public static SearchResult getAllPSUFor(String category, String version, String userId, String password) throws
            IOException {
        String releaseNumber = getReleaseNumber(category, version, userId, password);
        return getAllPSU(category, releaseNumber, userId, password);
    }

    /**
     * Get list of PSU available for given category and release.
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     * @throws IOException when failed
     */
    public static String getLatestPSUNumber(String category, String version, String userId, String password) {
        try {
            String releaseNumber = getReleaseNumber(category, version, userId, password);
            SearchResult searchResult = getAllPSU(category, releaseNumber, userId, password);
            if (searchResult.isSuccess()) {
                Document results = searchResult.getResults();
                return XPathUtil.applyXPathReturnString(results, "/results/patch[1]/name");
            }
        } catch (IOException | XPathExpressionException e) {
            //suppress exception
        }
        return null;
    }

    /**
     * Download a list of FMW patches.
     *
     * @param patches  A list of patches number
     * @param userId   user email
     * @param password password
     * @return List of bug numbers
     * @throws IOException when failed to access the aru api
     */
    public static List<String> getPatchesFor(List<String> patches, String userId, String password, String destDir)
            throws IOException {
        List<String> results = new ArrayList<>();
        for (String patch : patches) {
            String rs = getPatch(patch, userId, password, destDir);
            if (rs != null) {
                results.add(rs);
            }
        }
        return results;
    }

    /**
     * Validate patches conflicts by passing a list of patches.
     *
     * @param inventoryContent opatch lsinventory content (null if non is passed)
     * @param patches          A list of patches number
     * @param category         wls or fmw
     * @param version          version of the prduct
     * @param userId           userid for support account
     * @param password         password for support account
     * @return validationResult validation result object
     * @throws IOException when failed to access the aru api
     */

    public static ValidationResult validatePatches(String inventoryContent, List<String> patches, String category,
                                                   String version, String userId, String password) throws IOException {

        logger.finer("Entering ARUUtil.validatePatches");

        ValidationResult validationResult = new ValidationResult();
        validationResult.setSuccess(true);
        validationResult.setResults(null);
        if (userId == null || password == null) {
            logger.warning("No credentials provided, skipping validation of patches");
            return validationResult;
        }


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
            for (String patch : patches) {

                if (patch == null) {
                    logger.finest("Skipping null patch");
                    continue;
                }
                checkForMultiplePatches(patch, userId, password);
                logger.info("Passed patch multiple versions test");

                String bugReleaseNumber = ARUUtil.getPatchInfo(patch, userId, password);
                logger.info(String.format("Patch %s release %s found ", patch, bugReleaseNumber));
                int ind = patch.indexOf('_');
                String baseBugNumber = patch;
                if (ind > 0) {
                    baseBugNumber = patch.substring(0, ind);
                }
                payload.append(String.format("<patch_group rel_id=\"%s\">%s</patch_group>",
                        bugReleaseNumber, baseBugNumber));
            }
            payload.append("</candidate_patch_list>");
        }
        payload.append("</conflict_check_request>");

        logger.finer("Posting to ARU conflict check");
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
        logger.finer("Exiting ARUUtil.validatePatches");
        return validationResult;
    }

    /**
     * Return the patch detail.
     *
     * @param category  wls or fmw
     * @param version   version of the product
     * @param bugNumber bug number
     * @param userId    user id for support
     * @param password  password for support
     * @return dom document detail about the patch
     * @throws IOException when something goes wrong
     */
    public static SearchResult getPatchDetail(String category, String version, String bugNumber, String userId, String
            password)
            throws
            IOException {

        String releaseNumber = getReleaseNumber(category, version, userId, password);
        String url;
        if ("wls".equalsIgnoreCase(category))
            url = String.format(Constants.PATCH_SEARCH_URL, Constants.WLS_PROD_ID, bugNumber, releaseNumber);
        else
            url = String.format(Constants.PATCH_SEARCH_URL, Constants.FMW_PROD_ID, bugNumber, releaseNumber);

        return getSearchResult(HttpUtil.getXMLContent(url, userId, password));
    }

    /**
     * Return the version of the opatch patch.
     *
     * @param bugNumber opatch bug number
     * @param userId    user for support
     * @param password  password for support
     * @return version name of the this opatch patch
     * @throws IOException if XPath fails, and the patch release cannot be ascertained.
     */
    public static String getOPatchVersionByBugNumber(String bugNumber, String userId,
                                                     String password) throws IOException {

        try {
            String url = String.format(Constants.BUG_SEARCH_URL, bugNumber);
            Document allPatches = HttpUtil.getXMLContent(url, userId, password);
            return XPathUtil.applyXPathReturnString(allPatches, "string(/results/patch[1]/release/@name)");
        } catch (XPathExpressionException xe) {
            throw new IOException(xe.getMessage());
        }

    }


    private static SearchResult getSearchResult(Document result) throws IOException {
        SearchResult returnResult = new SearchResult();
        returnResult.setSuccess(true);

        try {
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(result, "/results/error");
            if (nodeList.getLength() > 0) {
                returnResult.setSuccess(false);
                returnResult.setErrorMessage(XPathUtil.applyXPathReturnString(result, "/results/error/message"));
            } else {
                returnResult.setResults(result);
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

        return returnResult;

    }


    private static Document getAllReleases(String category, String userId, String password) throws IOException {


        Document allReleases = HttpUtil.getXMLContent(Constants.REL_URL, userId, password);

        try {

            String expression;

            if ("wls".equalsIgnoreCase(category)) {
                expression = "/results/release[starts-with(text(), 'Oracle WebLogic Server')]";
            } else if (Constants.OPATCH_PATCH_TYPE.equalsIgnoreCase(category)) {
                expression = "/results/release[starts-with(text(), 'OPatch')]";
            } else {
                expression = "/results/release[starts-with(text(), 'Fusion Middleware Upgrade')]";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allReleases, expression);
            Document doc = createResultDocument(nodeList);
            return doc;

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    private static String getLatestPSU(String category, String release, String userId, String password, String destDir)
            throws
            IOException {

        String url;
        if ("wls".equalsIgnoreCase(category))
            url = String.format(Constants.LATEST_PSU_URL, Constants.WLS_PROD_ID, release);
        else
            url = String.format(Constants.LATEST_PSU_URL, Constants.FMW_PROD_ID, release);

        Document allPatches = HttpUtil.getXMLContent(url, userId, password);
        return savePatch(allPatches, userId, password, destDir, null);
    }

    private static SearchResult getAllPSU(String category, String release, String userId, String password) throws
            IOException {

        String url;
        if ("wls".equalsIgnoreCase(category))
            url = String.format(Constants.LATEST_PSU_URL, Constants.WLS_PROD_ID, release);
        else
            url = String.format(Constants.LATEST_PSU_URL, Constants.FMW_PROD_ID, release);

        return getSearchResult(HttpUtil.getXMLContent(url, userId, password));

    }

    private static String getPatch(String bugNumber, String userId, String password, String destDir)
            throws
            IOException {

        int ind = bugNumber.indexOf('_');
        String baseBugNumber = bugNumber;
        if (ind > 0) {
            baseBugNumber = bugNumber.substring(0, ind);
        }
        String url = String.format(Constants.BUG_SEARCH_URL, baseBugNumber);
        Document allPatches = HttpUtil.getXMLContent(url, userId, password);

        return savePatch(allPatches, userId, password, destDir, bugNumber);
    }

    private static void checkForMultiplePatches(String bugNumber, String userId, String password) throws IOException {
        if (bugNumber != null) {
            if (userId != null) {
                try {
                    int ind = bugNumber.indexOf('_');
                    String baseBugNumber = bugNumber;
                    String optionalRelease = null;
                    if (ind > 0) {
                        baseBugNumber = bugNumber.substring(0, ind);
                        optionalRelease = bugNumber.substring(ind + 1);
                    }
                    String url = String.format(Constants.BUG_SEARCH_URL, baseBugNumber);
                    Document allPatches = HttpUtil.getXMLContent(url, userId, password);
                    NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allPatches, "/results/patch");
                    if (nodeList.getLength() > 1 && ind < 0) {
                        // only base number is specified and there are multiple patches
                        // ERROR
                        String message = String.format("There are multiple patches found with id %s, please specify "
                                + "the format as one of the following for the release you want", baseBugNumber);
                        logger.warning(message);
                        for (int i = 1; i <= nodeList.getLength(); i++) {
                            String xpath = String.format("string(/results/patch[%d]/release/@name)", i);
                            String releaseNumber = XPathUtil.applyXPathReturnString(allPatches, xpath);
                            logger.warning(bugNumber + "_" + releaseNumber);
                        }
                        throw new IOException("Multiple patches with same patch number detected");
                    }

                } catch (XPathExpressionException xpe) {
                    throw new IOException(xpe);
                }
            }
        }

    }

    private static String savePatch(Document allPatches, String userId, String password, String destDir,
                                    String bugNumber) throws IOException {

        logger.finest("Saving patch " + bugNumber);
        try {

            if (bugNumber != null) {
                int ind = bugNumber.indexOf('_');
                String baseBugNumber = bugNumber;
                String optionalRelease = null;
                if (ind > 0) {
                    baseBugNumber = bugNumber.substring(0, ind);
                    optionalRelease = bugNumber.substring(ind + 1);
                }
                NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allPatches, "/results/patch");
                if (nodeList.getLength() > 1 && ind < 0) {
                    // only base number is specified and there are multiple patches
                    // ERROR
                    String message = String.format("There are multiple patches found with id %s, please specify "
                            + "the format as one of the following for the release you want", baseBugNumber);
                    logger.warning(message);
                    for (int i = 1; i <= nodeList.getLength(); i++) {
                        String xpath = String.format("string(/results/patch[%d]/release/@name)", i);
                        String releaseNumber = XPathUtil.applyXPathReturnString(allPatches, xpath);
                        logger.warning(bugNumber + "_" + releaseNumber);
                    }
                    throw new IOException("Multiple patches with same patch number detected");
                }
                if (optionalRelease != null) {
                    // TODO: do we need this ?
                    String xpath = String.format("/results/patch[release[@name='%s']]", optionalRelease);
                    NodeList matchedResult = XPathUtil.applyXPathReturnNodeList(allPatches, xpath);
                    allPatches = createResultDocument(matchedResult);
                    logger.finest(XPathUtil.prettyPrint(allPatches));
                }
            }

            String downLoadLink = XPathUtil.applyXPathReturnString(allPatches, "string"
                    + "(/results/patch[1]/files/file/download_url/text())");

            String downLoadHost = XPathUtil.applyXPathReturnString(allPatches, "string"
                    + "(/results/patch[1]/files/file/download_url/@host)");

            String key = bugNumber;

            int index = downLoadLink.indexOf("patch_file=");

            if (index > 0) {
                String fileName = destDir + File.separator + downLoadLink.substring(
                        index + "patch_file=".length());
                HttpUtil.downloadFile(downLoadHost + downLoadLink, fileName, userId, password);
                String result = key + "=" + fileName;
                logger.finest("savePatch returns " + result);
                return result;
            }

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

        logger.finest("savePatch returns nothing");
        return null;
    }

    /**
     * getPatchInfo returns the internal release number from a patch number (with optional release info).
     *
     * @param bugNumber patch id in the format of 9999999_1.1.1.1.9999
     * @param userId    user id for support
     * @param password  password for support
     * @return internal release number of this patch
     * @throws IOException when the patch is not found or failed to connect to ARU
     */
    public static String getPatchInfo(String bugNumber, String userId, String password)
            throws
            IOException {
        logger.finest("Entering getPatchInfo " + bugNumber);

        try {
            int ind = bugNumber.indexOf('_');
            String optionalRelease = null;
            String baseBugNumber = bugNumber;
            if (ind > 0) {
                baseBugNumber = bugNumber.substring(0, ind);
                optionalRelease = bugNumber.substring(ind + 1);
            }
            String url = String.format(Constants.BUG_SEARCH_URL, baseBugNumber);
            Document allPatches = HttpUtil.getXMLContent(url, userId, password);

            String xpath;
            if (optionalRelease != null)
                xpath = String.format("/results/patch/release[@name='%s']/@id", optionalRelease);
            else
                xpath = String.format("/results/patch/release/@id");

            logger.finest("applying xpath: " + xpath);
            String releaseNumber = XPathUtil.applyXPathReturnString(allPatches, xpath);
            logger.finest(String.format("Exiting getPatchInfo %s = %s ", bugNumber, releaseNumber));
            if (releaseNumber == null || "".equals(releaseNumber)) {
                String error = String.format("Patch id %s not found", bugNumber);
                logger.severe(error);
                throw new IOException(error);
            }
            return releaseNumber;

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

    }


    /**
     * Given a product category (wls, fmw, opatch) and version, determines the release number corresponding to that
     * in the ARU database.
     *
     * @param category wls, fmw, opatch
     * @param version  12.2.1.3.0 or such
     * @param userId   support email id
     * @param password password
     * @return release number
     * @throws IOException in case of error
     */
    public static String getReleaseNumber(String category, String version, String userId, String password) throws
            IOException {
        String key = category + CacheStore.CACHE_KEY_SEPARATOR + version;
        String retVal = releaseNumbersMap.getOrDefault(key, null);
        if (Utils.isEmptyString(retVal)) {
            Document allReleases = getAllReleases(category, userId, password);

            String expression = String.format("string(/results/release[@name = '%s']/@id)", version);
            try {
                retVal = XPathUtil.applyXPathReturnString(allReleases, expression);
            } catch (XPathExpressionException xpe) {
                throw new IOException(xpe);
            }
            if (!Utils.isEmptyString(retVal)) {
                releaseNumbersMap.put(category + CacheStore.CACHE_KEY_SEPARATOR + version, retVal);
            } else {
                throw new IOException(String.format("Failed to determine release number for category %s, version %s",
                        category, version));
            }
        }
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
        try {
            HttpUtil.getXMLContent(Constants.ARU_LANG_URL, username, password);
        } catch (IOException e) {
            Throwable cause = (e.getCause() == null) ? e : e.getCause();
            if (cause.getClass().isAssignableFrom(HttpResponseException.class)
                    && ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
        }
        return true;
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


    private static Document createResultDocument(NodeList nodeList) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element element = doc.createElement("results");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i);
                Node copyNode = doc.importNode(n, true);

                if (n instanceof Element)
                    element.appendChild(copyNode);
            }

            doc.appendChild(element);

            return doc;
        } catch (ParserConfigurationException pce) {
            throw new IOException(pce);
        }

    }
}

