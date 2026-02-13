// Copyright (c) 2019, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This class assists with requests made against the ARU server using the HTTPHelper. Common
 * information is encapsulated in the helper class. Checking for errors is also done in this class.
 */
public class AruHttpHelper {
    private boolean success;
    private final AruProduct product;
    private final String token;

    /**
     * Constructor encapsulating the information for the ARU search.
     *
     * @param product ARU product
     * @param token   ARU password credentials
     */
    AruHttpHelper(AruProduct product, String token) {
        this.product = product;
        this.token = token;
        this.success = true;
    }

    /**
     * For searches that only provide the credentials for the ARU searches.
     *
     * @param token ARU credentials password
     */
    AruHttpHelper(String token) {
        this(null, token);
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    boolean success() {
        return success;
    }

    /**
     * Return the ARU credentials password.
     *
     * @return oauth token
     */
    public String token() {
        return token;
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
        searchResult(HttpUtil.getXMLContent(url, token()));
        return this;
    }

    private void searchResult(Document result) throws IOException {
        success = true;
        try {
            NodeList nodeList = XPathUtil.nodelist(result, "/results/error");
            if (nodeList.getLength() > 0) {
                success = false;
            }
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }
    }


}
