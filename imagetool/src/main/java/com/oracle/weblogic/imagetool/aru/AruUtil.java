// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.cachestore.MultiplePatchVersionsException;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.oracle.weblogic.imagetool.util.Constants.ARU_LANG_URL;
import static com.oracle.weblogic.imagetool.util.Constants.CONFLICTCHECKER_URL;
import static com.oracle.weblogic.imagetool.util.Constants.GET_LSINVENTORY_URL;
import static com.oracle.weblogic.imagetool.util.Constants.RECOMMENDED_PATCHES_URL;
import static com.oracle.weblogic.imagetool.util.Constants.REL_URL;

public class AruUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);

    private static AruUtil instance;

    private static final String BUG_SEARCH_URL = "https://updates.oracle.com/Orion/Services/search?bug=%s";

    /**
     * Get ARU HTTP helper instance.
     * @return ARU helper.
     */
    public static AruUtil rest() {
        if (instance == null) {
            instance = new AruUtil();
        }
        return instance;
    }

    protected AruUtil() {
        // hide constructor
    }

    /**
     * Get list of PSU available for given product and version.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static List<AruPatch> getLatestPsu(FmwInstallerType type, String version, String userId, String password)
        throws Exception {
        List<AruPatch> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            List<AruPatch> psuList = getLatestPsu(new AruHttpHelper(product, version, userId, password));
            if (!psuList.isEmpty()) {
                for (AruPatch psu: psuList) {
                    String patchAndVersion = psu.patchId() + "_" + psu.version();
                    logger.info("IMG-0020", product.description(), patchAndVersion);
                    result.add(psu);
                }
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
     * Get the Latest PSU for the search information in the helper.
     *
     * @param aruHttpHelper containing product, version, credentials for the search
     * @return All ARU Patches that are labeled as a PSU bundle for this product
     * @throws Exception if the search or xml parse of result fails
     */
    static List<AruPatch> getLatestPsu(AruHttpHelper aruHttpHelper) throws Exception {
        logger.entering(aruHttpHelper.product());
        try {
            logger.info("IMG-0019", aruHttpHelper.product().description());
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPatchesMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                return AruPatch.getPatches(aruHttpHelper.results(), "[./psu_bundle]");
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
        return Collections.emptyList();
    }

    /**
     * Get list of recommended patches available for a given product and version.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public static List<AruPatch> getRecommendedPatches(FmwInstallerType type, String version,
                                                     String userId, String password) throws Exception {
        List<AruPatch> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            List<AruPatch> patches = getRecommendedPatches(new AruHttpHelper(product, version, userId, password));
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
    static List<AruPatch> getRecommendedPatches(AruHttpHelper aruHttpHelper)
        throws Exception {
        logger.entering(aruHttpHelper.product(), aruHttpHelper.version());
        try {
            logger.info("IMG-0067", aruHttpHelper.product().description());
            aruHttpHelper.release(getReleaseNumber(aruHttpHelper));
            getRecommendedPatchesMetadata(aruHttpHelper);
            if (aruHttpHelper.success()) {
                Document results = aruHttpHelper.results();
                List<AruPatch> patches = AruPatch.getPatches(results);
                logger.exiting(patches);
                return patches;
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
    public static void validatePatches(String inventoryContent, List<AruPatch> patches, String userId, String password)
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
    static void validatePatches(String inventoryContent, List<AruPatch> patches,
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

            AruHttpHelper upiResult = aruHttpHelper.execValidation(GET_LSINVENTORY_URL, upiPayload);

            NodeList upiList = XPathUtil.nodelist(upiResult.results(),
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
            for (AruPatch patch : patches) {
                if (patch == null) {
                    logger.finer("Skipping null patch");
                    continue;
                }

                logger.info("IMG-0022", patch.patchId(), patch.releaseName());

                payload.append(String.format("<patch_group rel_id=\"%s\">%s</patch_group>",
                        patch.release(), patch.patchId()));
            }
            payload.append("</candidate_patch_list>");
        }
        payload.append("</conflict_check_request>");

        logger.fine("Posting to ARU conflict check");
        aruHttpHelper = aruHttpHelper.execValidation(CONFLICTCHECKER_URL, payload.toString());
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
            aruHttpHelper.execSearch(REL_URL);
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
        String url = String.format(RECOMMENDED_PATCHES_URL, helper.product().productId(), helper.release());
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
            result = XPathUtil.string(allReleases, expression);
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
            aruHttpHelper.execSearch(ARU_LANG_URL);
        } catch (IOException e) {
            Throwable cause = (e.getCause() == null) ? e : e.getCause();
            if (cause.getClass().isAssignableFrom(HttpResponseException.class)
                    && ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                return false;
            }
        }
        return aruHttpHelper.success();
    }

    private void verifyResponse(Document response) throws IOException {
        try {
            NodeList nodeList = XPathUtil.nodelist(response, "/results/error");
            if (nodeList.getLength() > 0) {
                String errorMessage = XPathUtil.string(response, "/results/error/message");
                IOException ioe = new IOException(errorMessage);
                logger.throwing(ioe);
                throw ioe;
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }

    /**
     * Using a bug number, search ARU for a matching patches.
     * The same bug number can have multiple patches, one for each corresponding WLS version.
     * @param bugNumber the bug number to query ARU
     * @param userId user credentials with access to OTN
     * @param password password for the provided userId
     * @return an AruPatch
     * @throws IOException if there is an error retrieving the XML from ARU
     * @throws XPathExpressionException if AruPatch failed while extracting patch data from the XML
     */
    public List<AruPatch> getPatches(String bugNumber, String userId, String password)
        throws IOException, XPathExpressionException {
        return getPatches(bugNumber, userId, password, "");
    }

    private List<AruPatch> getPatches(String bugNumber, String userId, String password, String patchSelector)
        throws IOException, XPathExpressionException {

        if (userId == null || password == null) {
            // running in offline mode (no credentials to connect to ARU)
            return Collections.singletonList(new AruPatch().patchId(bugNumber));
        }

        String url = String.format(BUG_SEARCH_URL, bugNumber);
        logger.info("Searching Oracle for patch {0}", bugNumber);
        Document response = HttpUtil.getXMLContent(url, userId, password);
        verifyResponse(response);
        return AruPatch.getPatches(response, patchSelector);
    }

    /**
     * Using a bug number, search ARU for a matching patch.
     * @param bugNumber the bug number to query ARU
     * @param userId user credentials with access to OTN
     * @param password password for the provided userId
     * @return an AruPatch
     * @throws IOException if there is an error retrieving the XML from ARU
     * @throws XPathExpressionException if AruPatch failed while extracting patch data from the XML
     */
    public AruPatch getPatch(String bugNumber, String userId, String password, String patchSelector)
        throws IOException, XPathExpressionException {

        List<AruPatch> patches = getPatches(bugNumber, userId, password, patchSelector);

        if (patches.size() == 1) {
            return patches.get(0);
        } else {
            MultiplePatchVersionsException mpe = new MultiplePatchVersionsException(bugNumber, patches);
            logger.throwing(mpe);
            throw mpe;
        }
    }

    /**
     * Download a patch file from ARU.
     *
     * @param aruPatch ARU metadata for the patch
     * @param username userid for support account
     * @param password password for support account
     * @return path of the downloaded file
     * @throws IOException when it fails to access the url
     */

    public String downloadAruPatch(AruPatch aruPatch, String targetDir, String username, String password)
        throws IOException {

        logger.entering(aruPatch);

        // download the remote patch file to the local target directory
        String filename = targetDir + File.separator + aruPatch.fileName();
        logger.info("IMG-0018", aruPatch.patchId());
        try {
            Executor.newInstance(HttpUtil.getOraClient(username, password))
                .execute(Request.Get(aruPatch.downloadUrl()).connectTimeout(30000).socketTimeout(30000))
                .saveContent(new File(filename));
        } catch (Exception ex) {
            String message = String.format("Failed to download and save file %s from %s: %s", filename,
                aruPatch.downloadUrl(), ex.getLocalizedMessage());
            logger.severe(message);
            throw new IOException(message, ex);
        }
        logger.exiting(filename);
        return filename;
    }

    public static String getVersionSelector(String version) {
        return String.format("[./release[@name='%s']]", version);
    }


}

