// Copyright (c) 2020. 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public class AruPatch {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruPatch.class);

    private String patchId;
    private String version;
    private String description;
    private String product;
    private String release;
    private String releaseName;
    private String psuBundle;
    private String downloadHost;
    private String downloadPath;
    private String fileName;
    private String access;
    private String lifecycle;

    public String patchId() {
        return patchId;
    }

    public AruPatch patchId(String value) {
        patchId = value;
        return this;
    }

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

    public AruPatch lifecycle(String value) {
        lifecycle = value;
        return this;
    }

    public boolean isRecommended() {
        return "Recommended".equals(lifecycle);
    }

    /**
     * Given an XML document with a list of patches, extract each patch into the AruPatch bean and return the list.
     * @param patchList an XML document with a list of patches from ARU
     * @return a list of AruPatch
     * @throws XPathExpressionException if the document is not the expected format from ARU
     */
    public static List<AruPatch> getPatches(Document patchList) throws XPathExpressionException {
        return getPatches(patchList, "");
    }

    /**
     * Given an XML document with a list of patches, extract each patch into the AruPatch bean and return the list.
     * @param patchList an XML document with a list of patches from ARU
     * @param patchSelector additional XPath selector to limit the patches further
     * @return a list of AruPatch
     * @throws XPathExpressionException if the document is not the expected format from ARU
     */
    public static List<AruPatch> getPatches(Document patchList, String patchSelector) throws XPathExpressionException {
        // create list of all patches that apply to the Linux platform
        NodeList nodeList = XPathUtil.nodelist(patchList,
            "/results/patch[./platform[@id='2000' or @id='226']]" + patchSelector);
        List<AruPatch> result = new ArrayList<>();
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
                    .lifecycle(XPathUtil.string(nodeList.item(i), "./life_cycle"))
                    .downloadHost(XPathUtil.string(nodeList.item(i), "./files/file/download_url/@host"))
                    .downloadPath(XPathUtil.string(nodeList.item(i), "./files/file/download_url/text()"));

                int index = patch.downloadPath().indexOf("patch_file=");
                if (index < 0) {
                    throw new XPathExpressionException(Utils.getMessage("IMG-0059", patch.patchId()));
                }
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
        return result;
    }

    /**
     * Select a an ARU patch from the list based on a version number.
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
                                       String installerVersion) throws VersionNotFoundException {

        AruPatch selected = null;

        if (patches.isEmpty()) {
            return null;
        }

        if (patches.size() == 1) {
            selected = patches.get(0);
            if (selected.version() == null) {
                if (providedVersion != null) {
                    selected.version(providedVersion);
                } else if (psuVersion != null) {
                    selected.version(psuVersion);
                } else {
                    selected.version(installerVersion);
                }
            } else if (providedVersion != null && !selected.version().equals(providedVersion)) {
                throw new VersionNotFoundException(selected.patchId(), providedVersion, patches);
            }
            return selected;
        }

        Map<String, AruPatch> patchMap = patches.stream().collect(Collectors
            .toMap(AruPatch::version, aruPatch -> aruPatch));
        // if the user provided a specific version, select the provided version, or fail
        if (providedVersion != null) {
            if (patchMap.containsKey(providedVersion)) {
                selected = patchMap.get(providedVersion);
            } else {
                throw new VersionNotFoundException(patches.get(0).patchId(), providedVersion, patches);
            }
        } else if (patchMap.containsKey(psuVersion)) {
            selected = patchMap.get(psuVersion);
        } else if (patchMap.containsKey(installerVersion)) {
            selected = patchMap.get(installerVersion);
        }

        return selected;
    }

    @Override
    public String toString() {
        return patchId + " - " + description;

    }
}
