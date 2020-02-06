// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.protocol.HttpClientContext;
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

    private static final LoggingFacade logger = LoggingFactory.getLogger(HttpUtil.class);

    private static Document parseXmlString(String xmlString) throws ClientProtocolException {
        logger.entering();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
            } catch (Throwable ex) {
                logger.warning("Failed to set XML factory feature: {0}", ex.getLocalizedMessage());
            }

            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (Throwable ex) {
                logger.warning("Failed to set FEATURE_SECURE_PROCESSING: {0}", ex.getLocalizedMessage());
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource input = new InputSource(new StringReader(xmlString));
            Document doc = builder.parse(input);
            logger.finest(doc);
            logger.exiting();
            return doc;
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        } catch (Exception g) {
            throw new IllegalStateException(g);
        }
    }

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

        logger.entering(url);
        String xmlString = Executor.newInstance(getOraClient(username, password))
                .execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
                .returnContent().asString();
        logger.exiting();
        return parseXmlString(xmlString);
    }

    private static HttpClient getOraClient(String userId, String password) {
        logger.entering(userId);
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
        HttpClient result = HttpClientBuilder.create()
            .setDefaultRequestConfig(config.build())
            .setRetryHandler(retryHandler())
            .setDefaultCookieStore(cookieStore).useSystemProperties()
            .setDefaultCredentialsProvider(credentialsProvider).build();
        logger.exiting();
        return result;
    }

    private static HttpRequestRetryHandler retryHandler() {
        return (exception, executionCount, context) -> {

            if (executionCount > 3) {
                // Do not retry if over max retries
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake failed
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // return true if it is okay to retry this request type
            boolean retriable = !(request instanceof HttpEntityEnclosingRequest);
            if (retriable) {
                try {
                    logger.warning("Connect failed, retrying in 10 seconds, attempts={0} ", executionCount);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    //continue
                }
                return true;
            } else {
                return false;
            }
        };
    }

    /**
     * Download a file from the url.
     *
     * @param url      url of the aru server
     * @param fileName full path to save the file
     * @param username userid for support account
     * @param password password for support account
     * @throws IOException when it fails to access the url
     */

    public static void downloadFile(String url, String fileName, String username, String password)
            throws IOException {
        logger.entering(url);
        try {
            Executor.newInstance(getOraClient(username, password))
                    .execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
                    .saveContent(new File(fileName));
        } catch (Exception ex) {
            String message = String.format("Failed to download and save file %s from %s: %s", fileName, url,
                    ex.getLocalizedMessage());
            logger.severe(message);
            throw new IOException(message, ex);
        }
        logger.exiting(fileName);
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

        logger.entering(url, payload);
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
        logger.exiting();
        return parseXmlString(xmlString);

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
