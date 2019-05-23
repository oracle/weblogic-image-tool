/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.util;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ARUUtil {

    private static final Map<String, String> releaseNumbersMap = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ARUUtil.class.getName());

    /**
     * Return All WLS releases information
     *
     * @param userId   userid for support account
     * @param password password for support account
     * @throws IOException when failed to access the aru api
     */

    public static Document getAllWLSReleases(String userId, String password) throws IOException {
        return getAllReleases("wls", userId, password);
    }

    /**
     * Return release number of a WLS release by version
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
     * Return release number of a FMW release by version
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
     * Return All FMW releases information
     *
     * @param userId   userid for support account
     * @param password password for support account
     * @throws IOException when failed to access the aru api
     */
    public static void getAllFMWReleases(String userId, String password) throws IOException {
        getAllReleases("fmw", userId, password);
    }


    /**
     * Download the latest PSU for given category and release
     *
     * @param category wls or fmw
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @param password password
     * @return bug number
     * @throws IOException when failed
     */
    public static String getLatestPSUFor(String category, String version, String userId, String password, String destDir) throws IOException {
        String releaseNumber = getReleaseNumber(category, version, userId, password);
        return getLatestPSU(category, releaseNumber, userId, password, destDir);
    }

    /**
     * Get list of PSU available for given category and release
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
     * Get list of PSU available for given category and release
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
     * Download a list of FMW patches
     *
     * @param patches  A list of patches number
     * @param userId   user email
     * @param password password
     * @return List of bug numbers
     * @throws IOException when failed to access the aru api
     */
    public static List<String> getPatchesFor(String category, String version, List<String> patches, String userId, String password, String destDir)
            throws
            IOException {
        List<String> results = new ArrayList<>();
        for (String patch : patches) {
            String rs = getPatch(category, version, patch, userId, password, destDir);
            if (rs != null) {
                results.add(rs);
            }
        }
        return results;
    }

    /**
     * Validate patches conflicts by passing a list of patches
     *
     * @param lsInventoryPath opatch lsinventory result path (null if non is passed)
     * @param patches         A list of patches number
     * @param category        wls or fmw
     * @param version         version of the prduct
     * @param userId          userid for support account
     * @param password        password for support account
     * @return validationResult validation result object
     * @throws IOException when failed to access the aru api
     */

    public static ValidationResult validatePatches(String lsInventoryPath, List<String> patches, String category,
                                                   String version, String userId, String password) throws IOException {

        logger.finer("Entering ARUUtil.validatePatches");
        ValidationResult validationResult = new ValidationResult();
        validationResult.setSuccess(true);
        validationResult.setResults(null);

        String releaseNumber = getReleaseNumber(category, version, userId, password);

        StringBuffer payload = new StringBuffer
                ("<conflict_check_request><platform>2000</platform>");

        if (lsInventoryPath != null) {
            String inventoryContent = new String(Files.readAllBytes(Paths.get(lsInventoryPath)));
            String upiPayload = "<inventory_upi_request><lsinventory_output>" + inventoryContent +
                    "</lsinventory_output></inventory_upi_request>";

            Document upiResult = HttpUtil.postCheckConflictRequest(Constants.GET_LSINVENTORY_URL, upiPayload, userId,
                    password);

            try {
                NodeList upi_list = XPathUtil.applyXPathReturnNodeList(upiResult,
                        "/inventory_upi_response/upi");
                if (upi_list.getLength() > 0) {
                    payload.append("<target_patch_list>");

                    for (int ii = 0; ii < upi_list.getLength(); ii++) {
                        Node upi = upi_list.item(ii);
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
                payload.append(String.format("<patch_group rel_id=\"%s\">%s</patch_group>",
                        releaseNumber, patch));
            }
            payload.append("</candidate_patch_list>");
        }
        payload.append("</conflict_check_request>");

        logger.finer("Posting to ARU conflict check");
        Document result = HttpUtil.postCheckConflictRequest(Constants.CONFLICTCHECKER_URL, payload.toString(), userId, password);
        try {
            NodeList conflictSets = XPathUtil.applyXPathReturnNodeList(result, "/conflict_check/conflict_sets/set");
            if (conflictSets.getLength() > 0) {
                try {
                    validationResult.setSuccess(false);
                    String expression = "/conflict_check/conflict_sets/set/merge_patches";

                    NodeList nodeList = XPathUtil.applyXPathReturnNodeList(result, expression);

                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = dbf.newDocumentBuilder();
                    Document doc = builder.newDocument();
                    Element element = doc.createElement("conflict_check_results");

                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node n = nodeList.item(i);
                        Node copyNode = doc.importNode(n, true);

                        if (n instanceof Element)
                            element.appendChild(copyNode);
                    }

                    doc.appendChild(element);

                    validationResult.setResults(doc);
                    validationResult.setErrorMessage(parsePatchValidationError(doc));

                } catch (XPathExpressionException | ParserConfigurationException xpe) {
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
     * Return the patch detail
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
            } else if ("opatch".equalsIgnoreCase(category)) {
                expression = "/results/release[starts-with(text(), 'OPatch')]";
            } else {
                expression = "/results/release[starts-with(text(), 'Fusion Middleware Upgrade')]";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allReleases, expression);

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
            //XPathUtil.prettyPrint(doc);

            return doc;

        } catch (XPathExpressionException | ParserConfigurationException xpe) {
            throw new IOException(xpe);
        }
    }

    private static String getLatestPSU(String category, String release, String userId, String password, String destDir) throws
            IOException {

        String url;
        if ("wls".equalsIgnoreCase(category))
            url = String.format(Constants.LATEST_PSU_URL, Constants.WLS_PROD_ID, release);
        else
            url = String.format(Constants.LATEST_PSU_URL, Constants.FMW_PROD_ID, release);

        Document allPatches = HttpUtil.getXMLContent(url, userId, password);
        return savePatch(allPatches, userId, password, destDir);
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

    private static String getPatch(String category, String version, String bugNumber, String userId, String password, String destDir)
            throws
            IOException {

        String releaseNumber = getReleaseNumber(category, version, userId, password);
        String url;
        if ("wls".equalsIgnoreCase(category)) {
            url = String.format(Constants.PATCH_SEARCH_URL, Constants.WLS_PROD_ID, bugNumber, releaseNumber);
        } else if ("opatch".equalsIgnoreCase(category)) {
            url = String.format(Constants.PATCH_SEARCH_URL, Constants.OPATCH_PROD_ID, bugNumber, releaseNumber);
        } else {
            url = String.format(Constants.PATCH_SEARCH_URL, Constants.FMW_PROD_ID, bugNumber, releaseNumber);
        }
        Document allPatches = HttpUtil.getXMLContent(url, userId, password);

        return savePatch(allPatches, userId, password, destDir);
    }

    private static String savePatch(Document allPatches, String userId, String password, String destDir) throws IOException {
        try {

            // TODO: needs to make sure there is one and some filtering if not sorting

            String downLoadLink = XPathUtil.applyXPathReturnString(allPatches, "string"
                    + "(/results/patch[1]/files/file/download_url/text())");

            String downLoadHost = XPathUtil.applyXPathReturnString(allPatches, "string"
                    + "(/results/patch[1]/files/file/download_url/@host)");

            String bugName = XPathUtil.applyXPathReturnString(allPatches, "/results/patch[1]/name");

            String releaseNumber = XPathUtil.applyXPathReturnString(allPatches,
                    "string(/results/patch[1]/release/@id)");

            String key = bugName + CacheStore.CACHE_KEY_SEPARATOR + releaseNumber;

            int index = downLoadLink.indexOf("patch_file=");

            if (index > 0) {
                String fileName = destDir + File.separator + downLoadLink.substring(
                        index + "patch_file=".length());
                HttpUtil.downloadFile(downLoadHost + downLoadLink, fileName, userId, password);
                /*
                if (!new File(fileName).exists()) {
                    HttpUtil.downloadFile(downLoadHost + downLoadLink, fileName, userId, password);
                } else {
                    logger.info(String.format("patch %s already downloaded for bug %s", fileName, key));
                }*/
                return key + "=" + fileName;
            }

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

        return null;
    }

    /**
     * Given a product category (wls, fmw, opatch) and version, determines the release number corresponding to that
     * in the ARU database
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
     * Validates whether the given username and password are valid MOS credentials
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
            if (cause.getClass().isAssignableFrom(HttpResponseException.class) &&
                    ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses the patch conflict check result and returns a string of patch conflicts grouped by each conflict
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
                    NodeList bugNumbers = XPathUtil.applyXPathReturnNodeList(patchSets.item(i), "patch/bug/number/text()");
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
}

