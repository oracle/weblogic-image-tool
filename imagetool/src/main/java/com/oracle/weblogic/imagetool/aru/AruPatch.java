// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AruPatch {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruPatch.class);

    private String patchId;
    private String version;
    private String description;
    private String release;
    private String releaseName;
    private String psuBundle;
    private String downloadHost;
    private String downloadPath;

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

    public String downloadHost() {
        return downloadHost;
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
                    .psuBundle(XPathUtil.string(nodeList.item(i), "./psu_bundle"))
                    .downloadHost(XPathUtil.string(nodeList.item(i), "./files/file/download_url/@host"))
                    .downloadPath(XPathUtil.string(nodeList.item(i), "./files/file/download_url/text()"));
                logger.info("Found patch " + patch.patchId() + "_" + patch.version()
                    + " - " + patch.description()
                    + " - " + patch.release() + " : " + patch.releaseName()
                    + " - " + patch.psuBundle()
                    + " - " + patch.downloadHost() + patch.downloadPath());
                result.add(patch);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return patchId + " - " + description;

    }
}
