// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class assists with requests made against the ARU server using the HTTPHelper. Common
 * information is encapsulated in the helper class. Checking for errors is also done in this class.
 */
public class AruHttpHelper {
    private boolean success;
    private Document results;
    private String errorMessage;
    private AruProduct product;
    private String version;
    private String userId;
    private String password;
    private String release;

    /**
     * Constructor encapsulating the information for the ARU search.
     *
     * @param product ARU product
     * @param version of the product
     * @param userId ARU user credentials
     * @param password ARU password credentials
     */
    AruHttpHelper(AruProduct product, String version, String userId, String password) {
        this.product = product;
        this.version = version;
        this.userId = userId;
        this.password = password;
        this.success = true;
        this.results = null;
    }

    /**
     * For searches that only provide the credentials for the ARU searches.
     *
     * @param userId ARU credentials userid
     * @param password ARU credentials password
     */
    AruHttpHelper(String userId, String password) {
        this(null, null, userId, password);
    }
    
    /**
     * Get the error errorMessage.
     *
     * @return the search error message
     */
    String errorMessage() {
        return errorMessage;
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    boolean success() {
        return success;
    }

    /**
     * Get the result object from the search.
     *
     * @return dom document detailing about the conflicts
     */
    Document results() {
        return results;
    }

    /**
     * Return the product version number for the search.
     *
     * @return product version number
     */
    String version() {
        return version;
    }

    /**
     * Return the Release number for the category and version.
     *
     * @return ARU release number
     */
    String release() {
        return release;
    }

    /**
     * Set the release number for the product category and version.
     *
     * @param release set the product release number
     */
    AruHttpHelper release(String release) {
        this.release = release;
        return this;
    }

    /**
     * Return the user for the ARU credentials.
     *
     * @return user id
     */
    String userId() {
        return userId;
    }

    /**
     * Return the ARU credentials password.
     *
     * @return password
     */
    public String password() {
        return password;
    }

    /**
     * Return the ARU product.
     */
    public AruProduct product() {
        return product;
    }

    /**
     * Perform the ARU search, and parse the returned XML String into a document.
     *
     * @param url for the ARU Search
     * @return this instance of encapsulation of ARU search information
     * @throws IOException if the search fails
     */
    AruHttpHelper execSearch(String url) throws IOException {
        searchResult(HttpUtil.getXMLContent(url, userId(), password()));
        return this;
    }

    /**
     * Call ARU via the HTTPUtil methods and check the result for conflict messages about
     * patches not compatible.
     *
     * @param url to the ARU conflict patch numbers site.
     * @param payload XML string containing the patch number being validated
     * @return this instance of encapsulation of ARU search information
     * @throws IOException if the ARU request is not successful
     */
    AruHttpHelper execValidation(String url, String payload) throws IOException {
        results = HttpUtil.postCheckConflictRequest(url, payload, userId, password);
        return this;
    }

    /**
     * Check the resulting document from the execValidation for conflicts.
     *
     * @return this instance of the encapsulation of the validated results
     * @throws IOException if not able to parse the returned Document for conflict messages
     */
    AruHttpHelper validation() throws IOException {
        NodeList conflictSets;
        try {
            conflictSets = XPathUtil.nodelist(results(),
                "/conflict_check/conflict_sets/set");
        } catch (XPathExpressionException xee) {
            throw new IOException(xee);
        }
        if (conflictSets.getLength() > 0) {
            try {
                success = false;
                String expression = "/conflict_check/conflict_sets/set/merge_patches";

                NodeList nodeList = XPathUtil.nodelist(results(), expression);

                createResultDocument(nodeList);

                errorMessage = parsePatchValidationError();

            } catch (XPathExpressionException xpe) {
                throw new IOException(xpe);
            }
            return this;
        }
        return this;
    }

    /**
     * Create an XML document from an XML NodeList.
     * @param nodeList partial XML document
     * @return a Document based on the NodeList provided
     * @throws IOException if an XML parser exception occurs trying to build the document
     */
    AruHttpHelper createResultDocument(NodeList nodeList) throws IOException {
        try {
            Document doc = HttpUtil.documentBuilder().newDocument();
            Element element = doc.createElement("results");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i);
                Node copyNode = doc.importNode(n, true);

                if (n instanceof Element) {
                    element.appendChild(copyNode);
                }
            }

            doc.appendChild(element);

            results = doc;
        } catch (ParserConfigurationException pce) {
            throw new IOException(pce);
        }
        return this;
    }

    /**
     * Parses the patch conflict check result and returns a string of patch conflicts grouped by each conflict.
     *
     * @return String
     */
    private String parsePatchValidationError() {
        StringBuilder stringBuilder = new StringBuilder();
        Node conflictsResultNode = results();
        if (conflictsResultNode != null) {
            try {
                NodeList patchSets = XPathUtil.nodelist(conflictsResultNode, "//merge_patches");
                stringBuilder.append("patch conflicts detected: ");
                for (int i = 0; i < patchSets.getLength(); i++) {
                    stringBuilder.append("[");
                    NodeList bugNumbers = XPathUtil.nodelist(patchSets.item(i), "patch/bug/number"
                        + "/text()");
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

    private void searchResult(Document result) throws IOException {
        success = true;
        try {
            NodeList nodeList = XPathUtil.nodelist(result, "/results/error");
            if (nodeList.getLength() > 0) {
                success = false;
                errorMessage = XPathUtil.string(result, "/results/error/message");
            } else {
                results = result;
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }


}
