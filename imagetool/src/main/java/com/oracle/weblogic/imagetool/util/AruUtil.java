// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.cachestore.PatchFile;
import com.oracle.weblogic.imagetool.installer.AruProduct;
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

    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);

    private AruUtil() {
        // hide constructor, usage of this class is only static utilities
    }

    /**
     * Get list of PSU available for given product and version.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static List<String> getLatestPsuNumber(FmwInstallerType type, String version, String userId, String password)
        throws Exception {
        List<String> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            String patchNum = getLatestPsuNumber(new AruHttpHelper(product, version, userId, password));
            if (!Utils.isEmptyString(patchNum)) {
                String patchAndVersion = patchNum + "_" + version;
                logger.info("IMG-0020", product.description(), patchAndVersion);
                result.add(patchAndVersion);
            } else {
                logger.info("{0} - there are no recommended PSUs at this time.", product.description());
            }
        }
        if (result.isEmpty()) {
            logger.warning("IMG-0023", type, version);
        }
        return result;
    }

    /**
     * Get the Latest PSU patch number for the search information in the helper.
     *
     * @param aruHttpHelper containing product, version, credentials for the search
     * @return PSU patch number
     * @throws Exception if the search or xml parse of result fails
     */
    static String getLatestPsuNumber(AruHttpHelper aruHttpHelper) throws Exception {
        logger.entering(aruHttpHelper.product());
        try {
            logger.info("IMG-0019", aruHttpHelper.product().description());
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPatchesMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                Document results = aruHttpHelper.results();
                String result = XPathUtil.applyXPathReturnString(results, "/results/patch[./psu_bundle]/name");
                logger.exiting(result);
                return result;
            } else if (!Utils.isEmptyString(aruHttpHelper.errorMessage())) {
                logger.fine(aruHttpHelper.errorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0032", 
                    aruHttpHelper.product().description(), aruHttpHelper.version()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0032", 
                aruHttpHelper.product().description(), aruHttpHelper.version()), e);
        }
        logger.exiting();
        return null;
    }

    /**
     * Get list of recommended patches available for a given product and version.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static List<String> getRecommendedPatches(FmwInstallerType type, String version,
                                                     String userId, String password) throws Exception {
        List<String> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            List<String> patches = getRecommendedPatches(new AruHttpHelper(product, version, userId, password));
            if (!patches.isEmpty()) {
                result.addAll(patches);
            }
        }
        if (result.isEmpty()) {
            logger.warning("IMG-0069", type, version);
        }
        return result;
    }

    /**
     * Get the latest PSU along with recommended patches for the information in the search helper.
     *
     * @param aruHttpHelper containing the search type, version and credentials
     * @return List of recommended patches
     * @throws Exception the search or resulting parse failed
     */
    static List<String> getRecommendedPatches(AruHttpHelper aruHttpHelper)
        throws Exception {
        logger.entering(aruHttpHelper.product(), aruHttpHelper.version());
        try {
            logger.info("IMG-0067", aruHttpHelper.product().description());
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPatchesMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                Document results = aruHttpHelper.results();
                // xpath - get list of all patches that are platform "generic" or "Linux x86:64"
                NodeList nodeList = XPathUtil.applyXPathReturnNodeList(results,
                    "/results/patch[./platform[@id='2000' or @id='226']]/name");
                List<String> result = new ArrayList<>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        String patchId = nodeList.item(i).getTextContent();
                        patchId += "_" + aruHttpHelper.version();
                        logger.info("IMG-0068", aruHttpHelper.product().description(), patchId);
                        result.add(patchId);
                    }
                }
                logger.exiting(result);
                return result;
            } else if (!Utils.isEmptyString(aruHttpHelper.errorMessage())) {
                logger.fine(aruHttpHelper.errorMessage());
            } else {
                throw new Exception(Utils.getMessage("IMG-0070", 
                    aruHttpHelper.product().description(), aruHttpHelper.version()));
            }
        } catch (IOException | XPathExpressionException e) {
            throw new Exception(Utils.getMessage("IMG-0070", 
                aruHttpHelper.product().description(), aruHttpHelper.version()), e);
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
        throws IOException, XPathExpressionException {
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
                                       AruHttpHelper aruHttpHelper) throws IOException, XPathExpressionException {
        logger.entering(patches, aruHttpHelper);

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

                logger.info("IMG-0022", patch.getBugNumber(), patch.getReleaseName());

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

    private static Document allReleasesDocument = null;

    private static Document getAllReleases(AruHttpHelper aruHttpHelper) throws IOException {
        if (allReleasesDocument == null) {
            logger.fine("Retrieving product release numbers from Oracle...");
            aruHttpHelper.execSearch(Constants.REL_URL);
            if (aruHttpHelper.success()) {
                allReleasesDocument = aruHttpHelper.results();
            } else {
                throw new IOException(Utils.getMessage("IMG-0081"));
            }
        }
        return allReleasesDocument;
    }

    private static void getRecommendedPatchesMetadata(AruHttpHelper helper) throws IOException {

        logger.entering();
        String url = String.format(Constants.RECOMMENDED_PATCHES_URL, helper.product().productId(), helper.release());
        logger.finer("getting recommended patches info from {0}", url);

        helper.execSearch(url);
        logger.exiting();
    }

    /**
     * Get the release number for a given product and version.
     *
     * @param aruHttpHelper helper with information for release search
     * @return release number
     * @throws IOException in case of error
     */
    private static String getReleaseNumber(AruHttpHelper aruHttpHelper) throws IOException {
        logger.entering(aruHttpHelper.product());

        String result;
        Document allReleases = getAllReleases(aruHttpHelper);

        String expression = String.format("string(/results/release[starts-with(text(), '%s')][@name = '%s']/@id)",
            aruHttpHelper.product().description(), aruHttpHelper.version());
        try {
            result = XPathUtil.applyXPathReturnString(allReleases, expression);
            logger.fine("Release number for {0} is {1}", aruHttpHelper.product().description(), result);
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
        if (Utils.isEmptyString(result)) {
            throw new IOException(Utils.getMessage("IMG-0082", aruHttpHelper.product(), aruHttpHelper.version()));
        }
        logger.exiting(result);
        return result;
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

