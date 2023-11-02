// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Metadata for a patch, as defined by ARU.
 * Simple bean for holding metadata obtained from ARU for a given patch ID and version.
 */
public class AruPatch implements Comparable<AruPatch> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruPatch.class);

    private String patchId;
    private Version version;
    private String description;
    private String product;
    private String release;
    private String releaseName;
    private String psuBundle;
    private String downloadHost;
    private String downloadPath;
    private String fileName;
    private String access;

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
        if (version != null) {
            return version.toString();
        } else {
            return null;
        }
    }

    public AruPatch version(String value) {
        version = new Version(value);
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

    public String release() {
        return release;
    }

    public AruPatch release(String value) {
        release = value;
        return this;
    }


    public String releaseName() {
        return releaseName;
    }

    public AruPatch releaseName(String value) {
        releaseName = value;
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
        // create list of all patches that apply to the Linux platform
        NodeList nodeList = XPathUtil.nodelist(patchList,
            "/results/patch[./platform[@id='2000' or @id='226']]");
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
                    .downloadHost(XPathUtil.string(nodeList.item(i), "./files/file/download_url/@host"))
                    .downloadPath(XPathUtil.string(nodeList.item(i), "./files/file/download_url/text()"));

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
        throws VersionNotFoundException, MultiplePatchVersionsException {

        logger.entering(patches, providedVersion, psuVersion, installerVersion);
        AruPatch selected = null;

        if (patches.size() < 2) {
            // 0 or 1 patch, fill in a patch version and just return what have
            return selectPatchOffline(patches, providedVersion, psuVersion, installerVersion);
        }

        Map<String, AruPatch> patchMap = patches.stream().collect(Collectors
            .toMap(AruPatch::version, aruPatch -> aruPatch));
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
            throw logger.throwing(new MultiplePatchVersionsException(patches.get(0).patchId(), patches));
        } else {
            logger.info("IMG-0099", selected.patchId(), selected.version(), selected.description());
            return selected;
        }
    }

    private static AruPatch selectPatchOffline(List<AruPatch> patches, String providedVersion, String psuVersion,
                                               String installerVersion) throws VersionNotFoundException {
        AruPatch result = null;

        if (patches.isEmpty()) {
            logger.fine("Patches list is empty");
        } else if (patches.size() == 1) {
            result = patches.get(0);
            // if the version is filled in, we are working online and there is nothing more to do.
            if (result.version() == null) {
                // no version means the patch is from the cache. Set the version as best we can.
                // TODO: this could be improved if the cache was improved to store patch version.
                if (providedVersion != null) {
                    result.version(providedVersion);
                } else if (psuVersion != null) {
                    result.version(psuVersion);
                } else {
                    result.version(installerVersion);
                }
            } else if (providedVersion != null && !result.version().equals(providedVersion)) {
                throw logger.throwing(new VersionNotFoundException(result.patchId(), providedVersion, patches));
            } else {
                logger.info("IMG-0099", result.patchId(), result.version(), result.description());
            }
        }
        logger.exiting(result);
        return result;
    }

    @Override
    public String toString() {
        return patchId + " - " + description;
    }

    @Override
    public int compareTo(AruPatch obj) {
        return version.compareTo(obj.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AruPatch aruPatch = (AruPatch) o;
        return Objects.equals(patchId, aruPatch.patchId) && Objects.equals(version, aruPatch.version)
            && Objects.equals(release, aruPatch.release);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patchId, version, release);
    }
}
