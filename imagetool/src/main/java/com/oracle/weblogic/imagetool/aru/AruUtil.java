// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.github.mustachejava.DefaultMustacheFactory;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.Architecture;
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
import static com.oracle.weblogic.imagetool.util.Utils.not;

public class AruUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static final String GENERIC_NAME = "Generic";
    private static final String AMD_NAME = "linux/amd64";
    private static final String ARM_NAME = "linux/arm64";

    private static AruUtil instance;

    private static final String BUG_SEARCH_URL = ARU_REST_URL + "/search?bug=%s";

    private int restRetries = 10;
    private int restInterval = 500;

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
        final String retriesEnvVar = "WLSIMG_REST_RETRY_MAX";
        final String retriesString = System.getenv(retriesEnvVar);
        try {
            if (retriesString != null) {
                restRetries = Integer.parseInt(retriesString);
                if (restRetries < 1) {
                    restRetries = 10;
                    logger.severe("IMG-0109", retriesEnvVar, retriesString, 1, restRetries);
                }
                logger.fine("Retry max set to {0}", restRetries);
            }
        } catch (NumberFormatException nfe) {
            logger.warning("IMG-0108", retriesEnvVar, retriesString);
        }

        final String waitEnvVar = "WLSIMG_REST_RETRY_INTERVAL";
        final String waitString = System.getenv(waitEnvVar);
        try {
            if (waitString != null) {
                restInterval = Integer.parseInt(waitString);
                if (restInterval < 0) {
                    restInterval = 500;
                    logger.severe("IMG-0109", waitEnvVar, waitString, 0, restInterval);
                }
                logger.fine("Retry interval set to {0}", restInterval);
            }
        } catch (NumberFormatException nfe) {
            logger.warning("IMG-0108", waitEnvVar, waitString);
        }
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
    public List<AruPatch> getLatestPsu(FmwInstallerType type, String version, Architecture architecture,
                                       String userId, String password)
        throws AruException {
        List<AruPatch> result = new ArrayList<>();
        for (AruProduct product : type.products()) {

            String baseVersion = version;

            if (type == FmwInstallerType.IDM && product == AruProduct.JRF) {
                InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.IDM,
                    architecture, version);
                if (metaData != null && metaData.getBaseFMWVersion() != null) {
                    baseVersion = metaData.getBaseFMWVersion();
                }
            }

            List<AruPatch> psuList = getLatestPsu(product, baseVersion, architecture, userId, password);
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
    List<AruPatch> getLatestPsu(AruProduct product, String version, Architecture architecture,
                                String userId, String password)
        throws AruException {
        logger.entering(product, version);
        try {
            logger.info("IMG-0019", product.description());
            String releaseNumber = getReleaseNumber(product, version, userId, password);
            if (Utils.isEmptyString(releaseNumber)) {
                // ARU does not have a release number for the given product and version, return empty patch list
                logger.info(Utils.getMessage("IMG-0082", version, product.description()));
                return Collections.emptyList();
            }
            Document aruRecommendations = retry(
                () -> getRecommendedPatchesMetadata(product, releaseNumber, userId, password));
            logger.exiting();
            return AruPatch.getPatches(aruRecommendations)
                .filter(p -> p.isApplicableToTarget(architecture.getAruPlatform()))
                .filter(AruPatch::isPsu)
                .filter(not(AruPatch::isIrregularPatch))
                .collect(Collectors.toList());
        } catch (NoPatchesFoundException ex) {
            logger.exiting();
            return Collections.emptyList();
        } catch (RetryFailedException | XPathExpressionException e) {
            throw logger.throwing(
                new AruException(Utils.getMessage("IMG-0032", product.description(), version), e));
        }
    }

    /**
     * Get list of recommended patches available for all the products that are part of the FMW installer type.
     *
     * @param type FMW installer type
     * @param version  version number like 12.2.1.3.0
     * @param userId   user
     * @return Document listing of all patches (full details)
     */
    public List<AruPatch> getRecommendedPatches(FmwInstallerType type, String version, Architecture architecture,
                                                String userId, String password) throws AruException {
        List<AruPatch> result = new ArrayList<>();

        for (AruProduct product : type.products()) {

            String baseVersion = version;

            if (type == FmwInstallerType.IDM && product == AruProduct.JRF) {
                InstallerMetaData metaData = ConfigManager.getInstance().getInstallerForPlatform(InstallerType.IDM,
                    architecture, version);
                if (metaData != null && metaData.getBaseFMWVersion() != null) {
                    baseVersion = metaData.getBaseFMWVersion();
                }
            }

            List<AruPatch> patches = getRecommendedPatches(type, product, baseVersion, architecture, userId, password);

            if (!patches.isEmpty()) {
                patches.forEach(p -> logger.info("IMG-0068", product.description(), p.patchId(), p.description()));
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
    List<AruPatch> getRecommendedPatches(FmwInstallerType type, AruProduct product, String version,
                                        Architecture architecture, String userId, String password) throws AruException {
        logger.entering(product, version);
        List<AruPatch> patches = Collections.emptyList();
        try {
            logger.info("IMG-0067", product.description());
            String releaseNumber = getReleaseNumber(product, version, userId, password);
            if (Utils.isEmptyString(releaseNumber)) {
                // ARU does not have a release number for the given product and version, return an empty patch list
                logger.info(Utils.getMessage("IMG-0082", version, product.description()));
            } else {
                // Get a list of patches applicable to the given product and release number
                patches = getReleaseRecommendations(product, releaseNumber, architecture, userId, password);
                logger.fine("Search for {0} recommended patches returned {1}", product, patches.size());
                if (type == FmwInstallerType.OHS) {
                    // Workaround for the Apache Plugin patch currently uploaded as an OHS patch.
                    patches = patches.stream().filter(p ->
                        !p.description().contains("APACHE PLUGIN")).collect(Collectors.toList());
                }

                String psuVersion = getPsuVersion(product.description(), patches);
                if (psuVersion != null) {
                    // Check to see if there is a release with the PSU version that contains overlay patches.
                    logger.fine("Recommended patch list contains a PSU, getting recommendations for PSU version {0}",
                        psuVersion);
                    // Get the release number for the PSU version number
                    String psuReleaseNumber = getReleaseNumber(product, psuVersion, userId, password);
                    // If there is a release for the specific PSU, check it for overlay patches
                    if (!Utils.isEmptyString(psuReleaseNumber)) {
                        // Get recommended patches for PSU release (includes PSU overlay patches)
                        List<AruPatch> overlays =
                            getReleaseRecommendations(product, psuReleaseNumber, architecture, userId, password);
                        logger.fine("Search for PSU {0} overlay patches returned {1}", psuVersion, overlays.size());
                        patches.addAll(overlays);
                    } else {
                        // ARU does not have a release number for the PSU version found (no overlays needed)
                        logger.fine("PSU release was not found for {0} : {1}", product, psuVersion);
                    }
                }
            }
        } catch (NoPatchesFoundException npf) {
            logger.info("IMG-0069", product.description(), version);
        } catch (RetryFailedException | XPathExpressionException e) {
            throw new AruException(Utils.getMessage("IMG-0070", product.description(), version), e);
        }
        logger.exiting(patches);
        return patches;
    }


    private String getPsuVersion(String productName, Collection<AruPatch> patches) {
        String psuBundle = patches.stream()
            .map(AruPatch::psuBundle)
            .filter(not(Utils::isEmptyString))
            .findFirst()
            .orElse(null);

        if (psuBundle != null) {
            return psuBundle.substring(productName.length() + 1);
        }
        return null;
    }

    List<AruPatch> getReleaseRecommendations(AruProduct product, String releaseNumber, Architecture architecture,
                                             String userId, String password)
        throws AruException, XPathExpressionException, RetryFailedException {

        Document patchesDocument = retry(
            () -> getRecommendedPatchesMetadata(product, releaseNumber, userId, password));

        return AruPatch.getPatches(patchesDocument)
            .filter(p -> p.isApplicableToTarget(architecture.getAruPlatform()))
            .filter(not(AruPatch::isIrregularPatch)) // remove the Stack Patch Bundle patch, if returned
            // TODO: Need an option for the user to request the Coherence additional feature pack.
            .filter(not(AruPatch::isCoherenceFeaturePack)) // remove the Coherence feature pack, if returned
            .filter(p -> p.release().equals(releaseNumber))
            .collect(Collectors.toList());
    }

    static class PatchLists {
        List<InstalledPatch> installedPatches;
        List<AruPatch> candidatePatches;

        public PatchLists(List<InstalledPatch> installedPatches, List<AruPatch> candidatePatches) {
            this.installedPatches = installedPatches;
            this.candidatePatches = candidatePatches;
        }
    }

    /**
     * Check for patch conflicts by passing a list of patches.
     *
     * @param installedPatches opatch lsinventory content (null if none is passed)
     * @param patches          A list of patches number
     * @param userId           userId for support account
     * @param password         password for support account
     * @throws IOException when failed to access the aru api
     */
    public void validatePatches(List<InstalledPatch> installedPatches, List<AruPatch> patches, String userId,
                                       String password) throws IOException, AruException {
        logger.entering(installedPatches, patches, userId);

        if (userId == null || password == null) {
            logger.warning(Utils.getMessage("IMG-0033"));
            return;
        }
        logger.info("IMG-0012");

        // create XML payload for REST call
        StringWriter payload = new StringWriter();
        new DefaultMustacheFactory("templates")
            .compile("conflict-check.mustache")
            .execute(payload, new PatchLists(installedPatches, patches)).flush();

        logger.fine("Posting to ARU conflict check: {0}", payload.toString());
        // Use ARU conflict_check API to check provided patches and previously installed patches for conflicts
        try {
            Document conflictResults = retry(() -> patchConflictCheck(payload.toString(), userId, password));
            List<List<String>> conflictSets = getPatchConflictSets(conflictResults);

            if (conflictSets.isEmpty()) {
                logger.info("IMG-0006");
            } else {
                AruException ex = new PatchConflictException(conflictSets);
                logger.throwing(ex);
                throw ex;
            }
        }  catch (RetryFailedException e) {
            logger.warning("IMG-0115");
        }
    }

    Document patchConflictCheck(String payload, String userId, String password) throws IOException {
        return HttpUtil.postCheckConflictRequest(CONFLICTCHECKER_URL, payload, userId, password);
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
                allReleasesDocument = retry(() -> getAndVerify(REL_URL, userId, password));
            } catch (RetryFailedException e) {
                throw new AruException(Utils.getMessage("IMG-0081"));
            }
        }
        return allReleasesDocument;
    }

    // could be private, but leaving as protected for unit testing
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
    public boolean checkCredentials(String username, String password) {
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

    private Document getAndVerify(String url, String userId, String password)
        throws IOException, XPathExpressionException, AruException {
        Document response = HttpUtil.getXMLContent(url, userId, password);
        return verifyResponse(response);
    }

    private Document verifyResponse(Document response) throws AruException, XPathExpressionException {
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
        return response;
    }

    List<List<String>> getPatchConflictSets(Document conflictCheckResult) throws IOException {
        List<List<String>> result = new ArrayList<>();
        try {
            NodeList conflictSets = XPathUtil.nodelist(conflictCheckResult,"/conflict_check/conflict_sets/set");
            if (conflictSets.getLength() > 0) {
                for (int i = 0; i < conflictSets.getLength(); i++) {
                    NodeList bugNumbers =
                        XPathUtil.nodelist(conflictSets.item(i), "merge_patches/patch/bug/number/text()");
                    List<String> bugList = new ArrayList<>();
                    for (int j = 0; j < bugNumbers.getLength(); j++) {
                        bugList.add(bugNumbers.item(j).getNodeValue());
                    }
                    if (!bugList.isEmpty()) {
                        result.add(bugList);
                    }
                }
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
        return result;
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
    public Stream<AruPatch> getPatches(String bugNumber, String userId, String password)
        throws AruException, IOException, XPathExpressionException {

        if (userId == null || password == null) {
            return ConfigManager.getInstance().getAruPatchForBugNumber(bugNumber).stream();
        }

        String url = String.format(BUG_SEARCH_URL, bugNumber);
        logger.info("IMG-0063", bugNumber);
        try {
            Document response = retry(() -> getAndVerify(url, userId, password));
            return AruPatch.getPatches(response);
        } catch (NoPatchesFoundException patchEx) {
            throw new NoPatchesFoundException(Utils.getMessage("IMG-0086", bugNumber), patchEx);
        } catch (RetryFailedException retryEx) {
            throw new AruException(Utils.getMessage("IMG-0110", retryEx));
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
        try {
            return retry(() -> downloadPatch(aruPatch, targetDir, username, password));
        } catch (AruException | RetryFailedException e) {
            logger.severe("IMG-0120");
            throw logger.throwing(
                new FileNotFoundException(Utils.getMessage("IMG-0037", aruPatch.patchId(), aruPatch.version())));
        }

    }

    private String downloadPatch(AruPatch aruPatch, String targetDir, String username, String password)
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
            String message = Utils.getMessage("IMG-0107", filename, aruPatch.downloadUrl(), ex.getLocalizedMessage());
            logger.severe(message);
            throw new IOException(message, ex);
        }
        logger.exiting(filename);
        return filename;
    }

    /**
     * The maximum number of retries that will be attempted when trying to reach the ARU REST API method.
     * This value can be set by using the environment variable WLSIMG_REST_RETRY_MAX.
     *
     * @return The maximum number of retries to attempt.
     */
    public int getMaxRetries() {
        return restRetries;
    }

    /**
     * The time between each ARU REST retry.
     * This value can be set by using the environment variable WLSIMG_REST_RETRY_INTERVAL.
     *
     * @return The time to wait between each ARU REST API attempt during the retry loop.
     */
    public int getRetryInterval() {
        return restInterval;
    }

    /**
     * Similar to java.util.function.Supplier.
     * @param <T> return type of the supplied method.
     */
    private interface MethodToRetry<T> {
        T process() throws IOException, XPathExpressionException, AruException;
    }

    // create an environment variable that can override the tries count (undocumented)
    private static <T> T retry(MethodToRetry<T> call) throws AruException, RetryFailedException {
        for (int i = 0; i < rest().getMaxRetries(); i++) {
            try {
                return call.process();
            } catch (UnknownHostException e) {
                throw new AruException(e.getLocalizedMessage(), e);
            } catch (IOException | XPathExpressionException e) {
                logger.info("IMG-0106", e.getMessage(), (i + 1), rest().getMaxRetries());
            }
            try {
                if (rest().getRetryInterval() > 0) {
                    logger.finer("Waiting {0} ms before retry...", rest().getRetryInterval());
                    Thread.sleep(rest().getRetryInterval());
                }
            } catch (InterruptedException wakeAndAbort) {
                logger.warning("Process interrupted!");
                Thread.currentThread().interrupt();
            }
        }
        // When all retries are exhausted, raise an ARU exception to exit the process (give up)
        throw logger.throwing(new RetryFailedException());
    }

    /**
     * Get the translated aru platform.
     * @param id from aru id
     * @return text string fromm id number
     */
    public static String getAruPlatformName(String id) {
        if ("2000".equals(id)) {
            return GENERIC_NAME;
        }
        if ("541".equals(id)) {
            return ARM_NAME;
        }
        if ("226".equals(id)) {
            return AMD_NAME;
        }

        return GENERIC_NAME;
    }

    /**
     * Get the translated aru platform.
     * @param id from aru platform string
     * @return platform number
     */
    public static String getAruPlatformId(String id) {
        if (GENERIC_NAME.equalsIgnoreCase(id)) {
            return "2000";
        }
        if (ARM_NAME.equals(id)) {
            return "541";
        }
        if (AMD_NAME.equals(id)) {
            return "226";
        }

        return "2000";
    }

}

