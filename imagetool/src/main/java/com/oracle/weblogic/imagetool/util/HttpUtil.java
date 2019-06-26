// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HttpUtil {

    private static final Logger logger = Logger.getLogger(HttpUtil.class.getName());

    /**
     * Return the xml result of a GET from the url.
     *
     * @param url      url of the aru server
     * @param username userid for support account
     * @param password password for support account
     * @return xml dom document
     * @throws IOException when it fails to access the url
     */
    public static Document getXMLContent(String url, String username, String password) throws IOException {

        logger.finest("HTTPUtil.getXMLContent " + url);
        String xmlString = Executor.newInstance(getOraClient(username, password))
                .execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
                .returnContent().asString();
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document doc = docBuilder.parse(is);
            logger.finest("HTTPUtil.getXMLContent result " + XPathUtil.prettyPrint(doc));
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        } catch (Exception g) {
            throw new IllegalStateException(g);
        }
    }

    private static HttpClient getOraClient(String userId, String password) {
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);
        config.setCookieSpec(CookieSpecs.STANDARD);

        CookieStore cookieStore = new BasicCookieStore();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (userId != null && password != null) {
            BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
            cc.setDomain("edelivery.oracle.com");
            cookieStore.addCookie(cc);
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    userId, password));
        }
        return HttpClientBuilder.create().setDefaultRequestConfig(config.build())
                .setDefaultCookieStore(cookieStore).useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider).build();
    }

    /**
     * Downlod a file from the url.
     *
     * @param url      url of the aru server
     * @param fileName full path to save the file
     * @param username userid for support account
     * @param password password for support account
     * @throws IOException when it fails to access the url
     */

    public static void downloadFile(String url, String fileName, String username, String password)
            throws IOException {
        Executor.newInstance(getOraClient(username, password))
                .execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
                .saveContent(new File(fileName));
    }

    /**
     * Check conflicts post method.
     *
     * @param url      url for conflict checker api
     * @param payload  payload containing patches to check for conflicts
     * @param username user name for support
     * @param password password for support
     * @return dom document result of the conflict checker
     * @throws IOException if HTTP client fails
     */

    public static Document postCheckConflictRequest(String url, String payload, String username, String password)
            throws IOException {

        logger.finest("HTTPUtil.postCheckConflictRequest: " + payload);
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);
        config.setRedirectsEnabled(true);

        CookieStore cookieStore = new BasicCookieStore();

        CloseableHttpClient client =
                HttpClientBuilder.create().setDefaultRequestConfig(config.build()).useSystemProperties().build();

        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
        httpExecutor.use(cookieStore);

        // Has to do search first, otherwise results in 302
        // MUST use the same httpExecutor to maintain session


        httpExecutor
                .execute(Request.Get(Constants.REL_URL).connectTimeout(30000).socketTimeout(30000))
                .returnContent().asString();

        HttpEntity entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody("request_xml", payload)
                .build();

        String xmlString =
                httpExecutor.execute(Request.Post(url).connectTimeout(30000).socketTimeout(30000).body(entity))
                        .returnContent().asString();
        logger.finest("Returned Raw result " + xmlString);
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document doc = docBuilder.parse(is);
            logger.finest("HTTPUtil.postCheckConflictRequest result " + XPathUtil.prettyPrint(doc));
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        } catch (Exception g) {
            throw new IllegalStateException(g);
        }

    }

    /**
     * Looks at the GitHub repo and retrieves the tags from the source code.
     * @return a list of tags found.
     * @throws IOException if the HTTP client fails.
     */
    public static List<String> getWDTTags() throws IOException {
        List<String> retVal = new ArrayList<>();
        String results = Executor.newInstance(getOraClient(null, null)).execute(Request.Get(Constants.WDT_TAGS_URL))
                .returnContent().asString();
        JSONArray jsonArr = new JSONArray(results);
        for (int i = 0; i < jsonArr.length(); i++) {
            retVal.add(jsonArr.getJSONObject(i).getString("name"));
        }
        return retVal;
    }
}
