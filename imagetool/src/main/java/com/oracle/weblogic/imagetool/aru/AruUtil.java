// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static com.oracle.weblogic.imagetool.util.Constants.ARU_LANG_URL;
import static com.oracle.weblogic.imagetool.util.Constants.ARU_REST_URL;
import static com.oracle.weblogic.imagetool.util.Constants.CONFLICTCHECKER_URL;
import static com.oracle.weblogic.imagetool.util.Constants.RECOMMENDED_PATCHES_URL;
import static com.oracle.weblogic.imagetool.util.Constants.REL_URL;

public class AruUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);

    private static AruUtil instance;

    private static final String BUG_SEARCH_URL = ARU_REST_URL + "/search?bug=%s";

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
     * Get list of PSU available for each of the ARU products for the given FMW install type.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   OTN credential user
     * @param password OTN credential password
     * @return a list of patches from ARU
     * @throws AruException when an error occurs trying to access ARU metadata
     */
    public List<AruPatch> getLatestPsu(FmwInstallerType type, String version, String userId, String password)
        throws AruException {
        List<AruPatch> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            List<AruPatch> psuList = getLatestPsu(product, version, userId, password);
            if (!psuList.isEmpty()) {
                for (AruPatch psu: psuList) {
                    String patchAndVersion = psu.patchId() + "_" + psu.version();
                    logger.info("IMG-0020", product.description(), patchAndVersion);
                    result.add(psu);
                }
            } else {
                logger.info("IMG-0001", product.description(), version);
            }
        }
        if (result.isEmpty()) {
            logger.warning("IMG-0023", type, version);
        }
        return result;
    }

    /**
     * Get list of PSU available for given product and version.
     *
     * @param product  ARU product type, like WLS
     * @param version  version number like 12.2.1.3.0
     * @param userId   OTN credential user
     * @param password OTN credential password
     * @return the latest PSU for the given product and version
     * @throws AruException when response from ARU has an error or fails
     */
    List<AruPatch> getLatestPsu(AruProduct product, String version, String userId, String password)
        throws AruException {
        logger.entering(product, version);
        try {
            logger.info("IMG-0019", product.description());
            String releaseNumber = getReleaseNumber(product, version, userId, password);
            Document aruRecommendations = getRecommendedPatchesMetadata(product, releaseNumber, userId, password);
            logger.exiting();
            return AruPatch.getPatches(aruRecommendations, "[./psu_bundle]");
        } catch (NoPatchesFoundException | ReleaseNotFoundException ex) {
            logger.exiting();
            return Collections.emptyList();
        } catch (IOException | XPathExpressionException e) {
            throw new AruException(Utils.getMessage("IMG-0032", product.description(), version), e);
        }
    }

    /**
     * Get list of recommended patches available for a given product and version.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public List<AruPatch> getRecommendedPatches(FmwInstallerType type, String version,
                                                     String userId, String password) throws AruException {
        List<AruPatch> result = new ArrayList<>();
        for (AruProduct product : type.products()) {
            List<AruPatch> patches = getRecommendedPatches(product, version, userId, password);
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
     * Get list of recommended patches available for given product and version.
     *
     * @param product  ARU product type, like WLS
     * @param version  version number like 12.2.1.3.0
     * @param userId   OTN credential user
     * @param password OTN credential password
     * @return the recommended patches for the given product and version
     * @throws AruException when response from ARU has an error or fails
     */
    List<AruPatch> getRecommendedPatches(AruProduct product, String version, String userId, String password)
        throws AruException {
        logger.entering(product, version);
        try {
            logger.info("IMG-0067", product.description());
            String releaseNumber = getReleaseNumber(product, version, userId, password);
            Document aruRecommendations = getRecommendedPatchesMetadata(product, releaseNumber, userId, password);
            List<AruPatch> patches = AruPatch.getPatches(aruRecommendations);
            String psuVersion = getPsuVersion(patches);
            if (!Utils.isEmptyString(psuVersion)) {
                patches.forEach(p -> logger.fine("Discarding recommended patch {0} {1}", p.patchId(), p.description()));
                logger.fine("Recommended patch list contains a PSU, getting recommendations for PSU version {0}",
                    psuVersion);
                // get release number for PSU
                String psuReleaseNumber = getReleaseNumber(product, psuVersion, userId, password);
                // get recommended patches for PSU release (Overlay patches are only recommended on the PSU release)
                Document psuRecommendation = getRecommendedPatchesMetadata(product, psuReleaseNumber, userId, password);
                patches = AruPatch.getPatches(psuRecommendation);
            }
            patches.forEach(p -> logger.info("IMG-0068", product.description(), p.patchId(), p.description()));
            logger.exiting(patches);
            return patches;
        } catch (ReleaseNotFoundException nf) {
            return Collections.emptyList();
        } catch (NoPatchesFoundException npf) {
            logger.info("IMG-0069", product.description(), version);
            return Collections.emptyList();
        } catch (IOException | XPathExpressionException e) {
            throw new AruException(Utils.getMessage("IMG-0070", product.description(), version), e);
        }
    }

    private String getPsuVersion(List<AruPatch> patches) {
        for (AruPatch patch: patches) {
            if (patch.isPsu()) {
                // expected pattern "Oracle WebLogic Server 12.2.1.x.xxxxxx"
                String[] strings = patch.psuBundle().split(" ");
                return strings[strings.length - 1];
            }
        }
        return null;
    }

    /**
     * Validate patches conflicts by passing a list of patches.
     *
     * @param installedPatches opatch lsinventory content (null if none is passed)
     * @param patches          A list of patches number
     * @param userId           userId for support account
     * @param password         password for support account
     * @throws IOException when failed to access the aru api
     */
    public static void validatePatches(List<InstalledPatch> installedPatches, List<AruPatch> patches, String userId,
                                       String password) throws IOException {
        validatePatches(installedPatches, patches, new AruHttpHelper(userId, password));
    }

    /**
     * Validate patches conflict by passing a list of patches and encapsulated userId and password.
     *
     * @param installedPatches opatch lsinventory content (null if non is passed)
     * @param patches A list of patches number
     * @param aruHttpHelper encapsulated account credentials
     * @throws IOException when failed to access the aru api
     */
    static void validatePatches(List<InstalledPatch> installedPatches, List<AruPatch> patches,
                                       AruHttpHelper aruHttpHelper) throws IOException {
        logger.entering(patches, aruHttpHelper);

        if (aruHttpHelper.userId() == null || aruHttpHelper.password() == null) {
            logger.warning(Utils.getMessage("IMG-0033"));
            return;
        }
        logger.info("IMG-0012");

        StringBuilder payload = new StringBuilder("<conflict_check_request><platform>2000</platform>");

        if (installedPatches != null && !installedPatches.isEmpty()) {
            payload.append("<target_patch_list>");
            for (InstalledPatch patch : installedPatches) {
                payload.append(String.format("<installed_patch upi=\"%s\"/>", patch.getUniquePatchNumber()));
            }
            payload.append("</target_patch_list>");
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

    private Document allReleasesDocument = null;

    /**
     * Lookup all Oracle releases metadata from Oracle ARU.
     * Left as protected method to facilitate unit testing.
     *
     * @param userId   OTN credential user
     * @param password OTN credential password
     * @return the XML document from ARU with releases metadata
     * @throws AruException when ARU could not be reached or returns an error
     */
    Document getAllReleases(String userId, String password) throws AruException {
        if (allReleasesDocument == null) {
            logger.fine("Getting all releases document from ARU...");
            try {
                Document response = HttpUtil.getXMLContent(REL_URL, userId, password);
                verifyResponse(response);
                allReleasesDocument = response;
            } catch (IOException | AruException | XPathExpressionException ex) {
                throw new AruException(Utils.getMessage("IMG-0081"), ex);
            }
        }
        return allReleasesDocument;
    }

    Document getRecommendedPatchesMetadata(AruProduct product, String releaseNumber, String userId, String password)
        throws IOException, AruException, XPathExpressionException {

        logger.entering();
        String url = String.format(RECOMMENDED_PATCHES_URL, product.productId(), releaseNumber);
        logger.finer("getting recommended patches info from {0}", url);
        Document response = HttpUtil.getXMLContent(url, userId, password);
        verifyResponse(response);
        logger.exiting();
        return response;
    }

    /**
     * Get the release number for a given product and version.
     *
     * @param product  AruProduct type, like WLS
     * @param version  product version like 12.2.1.3.0
     * @param userId   OTN credential user
     * @param password OTN credential password
     * @return release number for the product and version provided
     * @throws AruException if the call to ARU fails, or the response from ARU had an error
     * @throws ReleaseNotFoundException if the specified version for the requested product was not found
     */
    private String getReleaseNumber(AruProduct product, String version, String userId, String password)
        throws AruException {
        logger.entering(product, version);

        String result;
        Document allReleases = getAllReleases(userId, password);

        String expression = String.format("string(/results/release[starts-with(text(), '%s %s')]/@id)",
            product.description(), version);
        try {
            result = XPathUtil.string(allReleases, expression);
            logger.fine("Release number for {0} is {1}", product.description(), result);
        } catch (XPathExpressionException xpe) {
            throw new AruException("Could not extract release number with XPath", xpe);
        }
        if (Utils.isEmptyString(result)) {
            String msg = Utils.getMessage("IMG-0082", version, product);
            logger.info(msg);
            throw new ReleaseNotFoundException(msg);
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

    void verifyResponse(Document response) throws AruException, XPathExpressionException {
        NodeList nodeList = XPathUtil.nodelist(response, "/results/error");
        if (nodeList.getLength() > 0) {
            String errorMessage = XPathUtil.string(response, "/results/error/message");
            logger.fine(errorMessage);
            String errorId = XPathUtil.string(response, "/results/error/id");
            AruException error;
            if ("10-016".equals(errorId)) {
                error = new NoPatchesFoundException(errorMessage);
            } else {
                error = new AruException(errorMessage);
            }
            logger.throwing(error);
            throw error;
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
        throws AruException, IOException, XPathExpressionException {

        if (userId == null || password == null) {
            // running in offline mode (no credentials to connect to ARU)
            return Collections.singletonList(new AruPatch().patchId(bugNumber));
        }

        String url = String.format(BUG_SEARCH_URL, bugNumber);
        logger.info("IMG-0063", bugNumber);
        Document response = HttpUtil.getXMLContent(url, userId, password);
        try {
            verifyResponse(response);
        } catch (NoPatchesFoundException patchEx) {
            throw new NoPatchesFoundException(Utils.getMessage("IMG-0086", bugNumber), patchEx);
        }
        return AruPatch.getPatches(response, "");
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
            HttpUtil.getHttpExecutor(username, password)
                .execute(Request.Get(aruPatch.downloadUrl()).connectTimeout(30000)
                    .socketTimeout(30000))
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
}

