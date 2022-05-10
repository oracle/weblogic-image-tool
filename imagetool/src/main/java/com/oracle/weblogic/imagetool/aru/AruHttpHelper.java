// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
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
    private final String userId;
    private final String password;

    /**
     * Constructor encapsulating the information for the ARU search.
     *
     * @param product ARU product
     * @param userId ARU user credentials
     * @param password ARU password credentials
     */
    AruHttpHelper(AruProduct product, String userId, String password) {
        this.product = product;
        this.userId = userId;
        this.password = password;
        this.success = true;
    }

    /**
     * For searches that only provide the credentials for the ARU searches.
     *
     * @param userId ARU credentials userid
     * @param password ARU credentials password
     */
    AruHttpHelper(String userId, String password) {
        this(null, userId, password);
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    boolean success() {
        return success;
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
