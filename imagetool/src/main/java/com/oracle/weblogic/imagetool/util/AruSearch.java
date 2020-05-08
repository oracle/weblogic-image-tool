// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class AruSearch {
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
    AruSearch(FmwInstallerType category, String version, String userid, String password) {
        this.category = category;
        this.version = version;
        this.userid = userid;
        this.password = password;
        this.success = true;
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
     * Set the error errorMessage.
     *
     * @param errorMessage message value.
     */
    AruSearch setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    boolean isSuccess() {
        return success;
    }

    AruSearch setSuccess(boolean success) {
        this.success = success;
        return this;
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
     * Set the result object from the search.
     *
     * @param results dom document detailing about the conflicts
     */
    AruSearch setResults(Document results) {
        this.results = results;
        return this;
    }

    /**
     * Return the FMWInstallerType category for the search.
     *
     * @return category
     */
    FmwInstallerType category() {
        return category;
    }

    /**
     * Set the FMWInstallerType for the search.
     *
     * @param category category type
     */
    AruSearch setCategory(FmwInstallerType category) {
        this.category = category;
        return this;
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
     * Set the product version number for the search.
     *
     * @param version product version number
     */
    AruSearch setVersion(String version) {
        this.version = version;
        return this;
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
    AruSearch setRelease(String release) {
        this.release = release;
        return this;
    }

    /**
     * Return the user for the ARU credentials.
     *
     * @return user id
     */
    String userId() {
        return userid;
    }

    /**
     * Set the ARU user id.
     *
     * @param userid user id
     */
    public AruSearch setUserId(String userid) {
        this.userid = userid;
        return this;
    }

    /**
     * Set the ARU credentials password.
     *
     * @return password
     */
    public String password() {
        return password;
    }

    /**
     * Set the ARU credentials password.
     *
     * @param password for ARU credentials
     */
    AruSearch setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Perform the ARU search, and parse the returned XML String into a document.
     *
     * @param url for the ARU Search
     * @throws IOException if the search fails
     */
    AruSearch execSearch(String url) throws IOException {
        setSearchResult(HttpUtil.getXMLContent(url, userId(), password()));
        return this;
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
