// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SearchHelper {
    private boolean success;
    private Document results;
    private String errorMessage;
    private FmwInstallerType category;
    private String version;
    private String userid;
    private String password;
    private String release;

    /**
     * Constructor encapsulating the information for the ARU search.
     *
     * @param category of the product
     * @param version of the product
     * @param userid ARU user credentials
     * @param password ARU password credentials
     */
    public SearchHelper(FmwInstallerType category, String version, String userid, String password) {
        this.category = category;
        this.version = version;
        this.userid = userid;
        this.password = password;
        this.success = true;
    }

    public SearchHelper() {
        // default Constructor
    }

    /**
     * Get the error errorMessage.
     *
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error errorMessage.
     *
     * @param errorMessage message value.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get the result object from the search.
     *
     * @return dom document detailing about the conflicts
     */
    public Document getResults() {
        return results;
    }

    /**
     * Set the result object from the search.
     *
     * @param results dom document detailing about the conflicts
     */
    public void setResults(Document results) {
        this.results = results;
    }

    /**
     * Return the FMWInstallerType category for the search.
     *
     * @return category
     */
    public FmwInstallerType getCategory() {
        return category;
    }

    /**
     * Set the FMWInstallerType for the search.
     *
     * @param category category type
     */
    public void setCategory(FmwInstallerType category) {
        this.category = category;
    }

    /**
     * Return the product version number for the search.
     *
     * @return product version number
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the product version number for the search.
     *
     * @param version product version number
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Return the Release number for the category and version.
     *
     * @return ARU release number
     */
    public String getRelease() {
        return release;
    }

    /**
     * Set the release number for the product category and version.
     *
     * @param release set the product release number
     */
    public void setRelease(String release) {
        this.release = release;
    }

    /**
     * Return the user for the ARU credentials.
     *
     * @return user id
     */
    public String getUserId() {
        return userid;
    }

    /**
     * Set the ARU user id.
     *
     * @param userid user id
     */
    public void setUserId(String userid) {
        this.userid = userid;
    }

    /**
     * Set the ARU credentials password.
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the ARU credentials password.
     *
     * @param password for ARU credentials
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Perform the ARU search, and parse the returned XML String into a document.
     *
     * @param url for the ARU Search
     * @throws IOException if the search fails
     */
    public void getXmlContent(String url) throws IOException {
        setSearchResult(HttpUtil.getXMLContent(url, getUserId(), getPassword()));
    }

    private void setSearchResult(Document result) throws IOException {
        setSuccess(true);
        try {
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(result, "/results/error");
            if (nodeList.getLength() > 0) {
                setSuccess(false);
                setErrorMessage(XPathUtil.applyXPathReturnString(result, "/results/error/message"));
            } else {
                setResults(result);
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }
}
