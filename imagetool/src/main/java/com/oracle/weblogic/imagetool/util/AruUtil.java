// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.PatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AruUtil {

    static final Map<String, String> releaseNumbersMap = new HashMap<>();
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);

    private AruUtil() {
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
    public static String getLatestPsuNumber(FmwInstallerType category, String version, String userId, String password)
        throws Exception {
        return getLatestPsuNumber(new AruHttpHelper(category, version, userId, password));
    }

    /**
     * Get the Latest PSU patch number for the search information in the helper.
     *
     * @param aruHttpHelper containing category, version, credentials for the search
     * @return PSU patch number
     * @throws Exception if the search or xml parse of result fails
     */
    static String getLatestPsuNumber(AruHttpHelper aruHttpHelper) throws Exception {
        logger.entering(aruHttpHelper);
        try {
            logger.info("IMG-0019");
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPsuMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                Document results = aruHttpHelper.results();
                String result = XPathUtil.applyXPathReturnString(results, "/results/patch[1]/name");
                logger.exiting(result);
                logger.info("IMG-0020", result);
                return result;
            } else if (!Utils.isEmptyString(aruHttpHelper.errorMessage())) {
                logger.warning("IMG-0023", aruHttpHelper.category(), aruHttpHelper.version());
                logger.fine(aruHttpHelper.errorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0032", 
                    aruHttpHelper.category(), aruHttpHelper.version()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0032", 
                aruHttpHelper.category(), aruHttpHelper.version()), e);
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
    public static List<String> getLatestPsuRecommendedPatches(FmwInstallerType category, String version,
                                                        String userId, String password) throws Exception {
        return getLatestPsuRecommendedPatches(new AruHttpHelper(category, version, userId, password));
    }

    /**
     * Get the latest PSU along with recommended patches for the information in the search helper.
     *
     * @param aruHttpHelper containing the search category, version and credentials
     * @return List of recommended patches
     * @throws Exception the search or resulting parse failed
     */
    static List<String> getLatestPsuRecommendedPatches(AruHttpHelper aruHttpHelper)
        throws Exception {
        logger.entering(aruHttpHelper);
        try {
            logger.info("IMG-0067");
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPatchesMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                Document results = aruHttpHelper.results();
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
            } else if (!Utils.isEmptyString(aruHttpHelper.errorMessage())) {
                logger.warning("IMG-0069", aruHttpHelper.category(), aruHttpHelper.version());
                logger.fine(aruHttpHelper.errorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0070", 
                    aruHttpHelper.category(), aruHttpHelper.version()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0070", 
                aruHttpHelper.category(), aruHttpHelper.version()), e);
        }
        logger.exiting();
        return Collections.emptyList();
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
        validatePatches(inventoryContent, patches, new AruHttpHelper(userId, password));
    }

    /**
     * Validate patches conflict by passing a list of patches and encapsulated userId and password.
     *
     * @param inventoryContent opatch lsinventory content (null if non is passed)
     * @param patches A list of patches number
     * @param aruHttpHelper encapsulated account credentials
     * @throws IOException when failed to access the aru api
     */
    static void validatePatches(String inventoryContent, List<PatchFile> patches,
                                       AruHttpHelper aruHttpHelper) throws IOException {
        logger.entering(inventoryContent, patches, aruHttpHelper);

        if (aruHttpHelper.userId() == null || aruHttpHelper.password() == null) {
            logger.warning(Utils.getMessage("IMG-0033"));
            return;
        }
        logger.info("IMG-0012");

        StringBuilder payload = new StringBuilder("<conflict_check_request><platform>2000</platform>");

        if (inventoryContent != null) {
            String upiPayload = "<inventory_upi_request><lsinventory_output>" + inventoryContent
                    + "</lsinventory_output></inventory_upi_request>";

            AruHttpHelper upiResult = aruHttpHelper.execValidation(Constants.GET_LSINVENTORY_URL, upiPayload);

            try {
                NodeList upiList = XPathUtil.applyXPathReturnNodeList(upiResult.results(),
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
        aruHttpHelper = aruHttpHelper.execValidation(Constants.CONFLICTCHECKER_URL, payload.toString());
        aruHttpHelper.validation();

        if (aruHttpHelper.success()) {
            logger.info("IMG-0006");
        } else {
            String error = aruHttpHelper.errorMessage();
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }
        logger.exiting(aruHttpHelper);
    }

    private static Document getAllReleases(AruHttpHelper aruHttpHelper) throws IOException {
        logger.entering(aruHttpHelper);
        aruHttpHelper.execSearch(Constants.REL_URL);

        try {

            String expression;

            if (FmwInstallerType.WLS == aruHttpHelper.category()) {
                expression = "/results/release[starts-with(text(), 'Oracle WebLogic Server')]";
            } else if (Constants.OPATCH_PATCH_TYPE.equalsIgnoreCase(aruHttpHelper.category().toString())) {
                expression = "/results/release[starts-with(text(), 'OPatch')]";
            } else {
                expression = "/results/release[starts-with(text(), 'Fusion Middleware Upgrade')]";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(aruHttpHelper.results(), expression);
            aruHttpHelper = aruHttpHelper.createResultDocument(nodeList);
            logger.exiting();
            return aruHttpHelper.results();

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    private static void getRecommendedPsuMetadata(AruHttpHelper aruHttpHelper) throws IOException {

        logger.entering();
        String productId = FmwInstallerType.WLS
            == aruHttpHelper.category() ? Constants.WLS_PROD_ID : Constants.FMW_PROD_ID;
        String url = String.format(Constants.RECOMMENDED_PATCHES_URL, productId, aruHttpHelper.release())
            + Constants.ONLY_GET_RECOMMENDED_PSU;
        logger.finer("getting PSU info from {0}", url);

        aruHttpHelper.execSearch(url);
        logger.exiting();
    }

    private static void getRecommendedPatchesMetadata(AruHttpHelper aruHttpHelper) throws IOException {

        logger.entering();
        String productId = FmwInstallerType.WLS
            == aruHttpHelper.category() ? Constants.WLS_PROD_ID : Constants.FMW_PROD_ID;
        String url = String.format(Constants.RECOMMENDED_PATCHES_URL, productId, aruHttpHelper.release());
        logger.finer("getting recommended patches info from {0}", url);

        aruHttpHelper.execSearch(url);
        logger.exiting();
    }

    /**
     * Given a product category (wls, fmw, opatch) and version, determines the release number corresponding to that
     * in the ARU database.
     *
     * @param aruHttpHelper helper with information for release search
     * @return release number
     * @throws IOException in case of error
     */
    private static String getReleaseNumber(AruHttpHelper aruHttpHelper)
        throws IOException {
        logger.entering(aruHttpHelper);
        String key = aruHttpHelper.category() + CacheStore.CACHE_KEY_SEPARATOR + aruHttpHelper.version();
        String retVal = releaseNumbersMap.getOrDefault(key, null);
        if (Utils.isEmptyString(retVal)) {
            logger.fine("Retrieving product release numbers from Oracle...");
            Document allReleases = getAllReleases(aruHttpHelper);

            String expression = String.format("string(/results/release[@name = '%s']/@id)", aruHttpHelper.version());
            try {
                retVal = XPathUtil.applyXPathReturnString(allReleases, expression);
                logger.fine("Release number for {0} is {1}", aruHttpHelper.category(), retVal);
            } catch (XPathExpressionException xpe) {
                throw new IOException(xpe);
            }
            if (!Utils.isEmptyString(retVal)) {
                releaseNumbersMap.put(aruHttpHelper.category()
                    + CacheStore.CACHE_KEY_SEPARATOR + aruHttpHelper.version(), retVal);
            } else {
                throw new IOException(String.format("Failed to determine release number for category %s, version %s",
                        aruHttpHelper.category(), aruHttpHelper.version()));
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
        AruHttpHelper aruHttpHelper = new AruHttpHelper(username, password);
        try {
            aruHttpHelper.execSearch(Constants.ARU_LANG_URL);
        } catch (IOException e) {
            Throwable cause = (e.getCause() == null) ? e : e.getCause();
            if (cause.getClass().isAssignableFrom(HttpResponseException.class)
                    && ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
        }
        return aruHttpHelper.success();
    }
}

