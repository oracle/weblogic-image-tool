// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.oracle.weblogic.imagetool.aru.AruUtil.getAruPlatformName;

/**
 * Metadata for a patch, as defined by ARU.
 * Simple bean for holding metadata obtained from ARU for a given patch ID and version.
 */
public class AruPatch {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruPatch.class);

    private String patchId;
    private String version;
    private String description;
    private String product;
    private String release;
    private String releaseName;
    private int platform;
    private String psuBundle;
    private String downloadHost;
    private String downloadPath;
    private String fileName;
    private String access;
    private String platformName = "Generic";
    private String sha256Hash;
    private String releasedDate;

    public String patchId() {
        return patchId;
    }

    public AruPatch patchId(String value) {
        patchId = value;
        return this;
    }

    /**
     * The ARU version number of the FMW product associated with this patch.
     * @return The string value of the version found in ARU.
     */
    public String version() {
        return version;
    }

    public AruPatch version(String value) {
        version = value;
        return this;
    }

    public String description() {
        return description;
    }


    public AruPatch description(String value) {
        description = value;
        return this;
    }

    public String product() {
        return product;
    }

    public AruPatch product(String value) {
        product = value;
        return this;
    }

    public String platformName() {
        return platformName;
    }

    public AruPatch platformName(String platform) {
        this.platformName = platform;
        return this;
    }

    public String release() {
        return release;
    }

    public AruPatch release(String value) {
        release = value;
        return this;
    }

    public AruPatch sha256Hash(String value) {
        sha256Hash = value;
        return this;
    }

    public String sha256Hash() {
        return sha256Hash;
    }

    public String releaseName() {
        return releaseName;
    }

    public AruPatch releaseName(String value) {
        releaseName = value;
        return this;
    }

    public Integer platform() {
        return platform;
    }

    /**
     * Setting platform value.
     * @param value value of the platform
     * @return this
     */
    public AruPatch platform(String value) {
        platform = Integer.parseInt(value);
        platformName = getAruPlatformName(value);
        return this;
    }

    public String psuBundle() {
        return psuBundle;
    }

    public AruPatch psuBundle(String value) {
        psuBundle = value;
        return this;
    }

    public String psuVersion() {
        return psuBundle.substring(psuBundle.lastIndexOf(' ') + 1);
    }

    public boolean isPsu() {
        return !Utils.isEmptyString(psuBundle);
    }

    public AruPatch downloadHost(String value) {
        downloadHost = value;
        return this;
    }

    public String downloadPath() {
        return downloadPath;
    }

    public AruPatch downloadPath(String value) {
        downloadPath = value;
        return this;
    }

    public AruPatch releasedDate(String value) {
        releasedDate = value;
        return this;
    }

    public String releasedDate() {
        return Utils.getReleaseDate(releasedDate);
    }

    public String downloadUrl() {
        return downloadHost + downloadPath;
    }

    public AruPatch fileName(String value) {
        fileName = value;
        return this;
    }

    public String fileName() {
        return fileName;
    }

    public AruPatch access(String value) {
        access = value;
        return this;
    }

    public String access() {
        return access;
    }

    public boolean isOpenAccess() {
        return "Open access".equals(access);
    }

    public boolean isStackPatchBundle() {
        return description != null && description.contains("STACK PATCH BUNDLE");
    }

    /**
     * Returns true if this patch is known irregular patch (not an actual patch).
     * <ol>
     *     <li>Stack Patch Bundle is a zip of patches, but is not a patch itself.</li>
     *     <li>DB Client 19c Upgrade (34761383) is an installer, and not a patch.</li>
     * </ol>
     * @return true if this patch is a StackPatchBundle or known installer, false otherwise.
     */
    public boolean isIrregularPatch() {
        boolean result = "34761383".equals(patchId) || isStackPatchBundle();
        if (result) {
            logger.fine("Detected irregular patch {0}: {1}", patchId, description);
        }
        return result;
    }

    public boolean isCoherenceFeaturePack() {
        return description != null && description.contains("Coherence 14.1.1 Feature Pack");
    }

    /**
     * Given an XML document with a list of patches, extract each patch into the AruPatch bean and return the list.
     * @param patchList an XML document with a list of patches from ARU
     * @return a list of AruPatch
     * @throws XPathExpressionException if the document is not the expected format from ARU
     */
    public static Stream<AruPatch> getPatches(Document patchList) throws XPathExpressionException {
        // create list of all patches that apply (platforms 2000=generic, 226=amd64, 541=arm64)
        NodeList nodeList = XPathUtil.nodelist(patchList,
            "/results/patch[./platform[@id='2000' or @id='226' or @id='541']]");
        Stream.Builder<AruPatch> result = Stream.builder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                AruPatch patch = new AruPatch()
                    .patchId(XPathUtil.string(nodeList.item(i), "./name"))
                    .version(XPathUtil.string(nodeList.item(i), "./release/@name"))
                    .release(XPathUtil.string(nodeList.item(i), "./release/@id"))
                    .releaseName(XPathUtil.string(nodeList.item(i), "./release/text()"))
                    .description(XPathUtil.string(nodeList.item(i), "./bug/abstract"))
                    .product(XPathUtil.string(nodeList.item(i), "./product/@id"))
                    .psuBundle(XPathUtil.string(nodeList.item(i), "./psu_bundle"))
                    .access(XPathUtil.string(nodeList.item(i), "./access"))
                    .sha256Hash(XPathUtil.string(nodeList.item(i),
                        "./files/file/digest[@type='SHA-256']/text()"))
                    .platformName(getAruPlatformName(XPathUtil.string(nodeList.item(i), "./platform/@id")))
                    .downloadHost(XPathUtil.string(nodeList.item(i), "./files/file/download_url/@host"))
                    .downloadPath(XPathUtil.string(nodeList.item(i), "./files/file/download_url/text()"))
                    .releasedDate(XPathUtil.string(nodeList.item(i), "./released_date/text()"))
                    .platform(XPathUtil.string(nodeList.item(i), "./platform/@id"));

                int index = patch.downloadPath().indexOf("patch_file=");
                if (index < 0) {
                    logger.fine("Unusable patch data from ARU for id:" + patch.patchId()
                        + "  ver:" + patch.version() + "  url:" + patch.downloadUrl());
                } else {
                    patch.fileName(patch.downloadPath().substring(index + "patch_file=".length()));

                    logger.fine("AruPatch created id:" + patch.patchId()
                        + "  ver:" + patch.version()
                        + "  desc:" + patch.description()
                        + "  rel:" + patch.release()
                        + "  product:" + patch.product()
                        + "  platform:" + patch.platform()
                        + "  relName:" + patch.releaseName()
                        + "  psu:" + patch.psuBundle()
                        + "  url:" + patch.downloadUrl());
                    result.add(patch);
                }
            }
        }
        return result.build();
    }

    /**
     * Select an ARU patch from the list based on a version number.
     * Version preference is: provided version, PSU version, and then installer version.
     * If there is only one patch in the list, no version checking is done, and that patch is returned.
     * @param patches list of patches to search
     * @param providedVersion user specified patch version, like bugnumber_12.2.1.4.0
     * @param psuVersion version number of the PSU, if applicable
     * @param installerVersion version of WebLogic installed or to be installed in the Oracle Home
     * @return the selected patch, or null
     * @throws VersionNotFoundException if the user requested a version that was not in the list.
     */
    public static AruPatch selectPatch(List<AruPatch> patches, String providedVersion, String psuVersion,
                                       String installerVersion)
        throws VersionNotFoundException, PatchVersionException {

        logger.entering(patches, providedVersion, psuVersion, installerVersion);
        AruPatch selected = null;

        Map<String, AruPatch> patchMap = new HashMap<>();
        for (AruPatch patch: patches) {
            if (patchMap.containsKey(patch.version())) {
                throw new IllegalStateException(Utils.getMessage("IMG-0122", patch.patchId(), patch.version()));
            }
            patchMap.put(patch.version(), patch);
        }

        // select the correct patch version (priority order: user provided version, PSU version, GA installer version)
        if (providedVersion != null) {
            // if the user provided a specific version, select the provided version, or fail
            if (patchMap.containsKey(providedVersion)) {
                selected = patchMap.get(providedVersion);
            } else {
                throw new VersionNotFoundException(patches.get(0).patchId(), providedVersion, patches);
            }
        } else if (patchMap.containsKey(psuVersion)) {
            // if the image has a PSU installed, or the new patch list has a PSU, otherwise skip to installer version
            selected = patchMap.get(psuVersion);
        } else if (patchMap.containsKey(installerVersion)) {
            selected = patchMap.get(installerVersion);
        }
        logger.exiting(selected);
        if (selected == null) {
            List<String> versionStrings = new ArrayList<>();
            for (AruPatch aruPatch : patches) {
                versionStrings.add(patches.get(0).patchId() + "_" + aruPatch.version());
            }
            throw logger.throwing(new PatchVersionException(patches.get(0).patchId(), versionStrings));
        } else {
            logger.info("IMG-0099", selected.patchId(), selected.version(), selected.description());
            return selected;
        }
    }

    /**
     * Return true if this patch is applicable to the target platform.
     * Patch platform 2000 is generic and applicable to all platforms.
     * @param aruPlatform the target ARU platform to check.
     * @return true if this patch is applicable to the provided platform.
     */
    public boolean isApplicableToTarget(int aruPlatform) {
        logger.finer("AruPatch id {0} platform {1} checking against {2}", patchId, platform, aruPlatform);
        // if this patch is for platform 2000, always return true, else return true if platforms are equal.
        return platform == 2000 || platform == aruPlatform;
    }

    @Override
    public String toString() {
        return "AruPatch{"
            + "patchId='" + patchId + '\''
            + ", version='" + version + '\''
            + ", description='" + description + '\''
            + ", product='" + product + '\''
            + ", release='" + release + '\''
            + ", releaseName='" + releaseName + '\''
            + ", psuBundle='" + psuBundle + '\''
            + ", downloadHost='" + downloadHost + '\''
            + ", downloadPath='" + downloadPath + '\''
            + ", fileName='" + fileName + '\''
            + ", access='" + access + '\''
            + ", platform='" + platform + '\''
            + ", platformName='" + platformName + '\''
            + ", sha256Hash='" + sha256Hash + '\''
            + '}';
    }
}
