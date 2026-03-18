// Copyright (c) 2019, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HttpUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(HttpUtil.class);
    private static final Timeout REQUEST_TIMEOUT = Timeout.ofSeconds(30);

    private HttpUtil() {
        // utility class with static methods
    }

    private static DocumentBuilderFactory builderFactory = null;

    /**
     * Using cached DocumentBuilderFactor, create a new instance of a DocumentBuilder for parsing XML.
     * @return a new instance of a DocumentBuilder
     * @throws ParserConfigurationException if the underlying JVM XML parser configuration throws an error
     */
    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        if (builderFactory == null) {
            builderFactory = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks
            builderFactory.setXIncludeAware(false);
            builderFactory.setExpandEntityReferences(false);
            builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        }
        return builderFactory.newDocumentBuilder();
    }

    public static Document parseXml(byte[] input) throws ClientProtocolException {
        return parseXml(new InputSource(new ByteArrayInputStream(input)));
    }

    public static Document parseXml(String input) throws ClientProtocolException {
        return parseXml(new InputSource(new StringReader(input)));
    }

    /**
     * Parse input into an XML Document.
     * @param input well formatted XML
     * @return org.w3c.dom.Document built from the provided String
     * @throws ClientProtocolException if the String contains malformed XML
     */
    public static Document parseXml(InputSource input) throws ClientProtocolException {
        try {
            Document doc = documentBuilder().parse(input);
            logger.finest(doc);
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
        String xmlString = getHttpExecutor(username, password)
            .execute(Request.get(url).connectTimeout(REQUEST_TIMEOUT).responseTimeout(REQUEST_TIMEOUT))
            .returnContent().asString();
        logger.finest(xmlString);
        logger.exiting();
        return parseXml(xmlString);
    }

    /**
     * Create an HTTP client with cookie and credentials for Oracle eDelivery.
     * @param userId Oracle credential
     * @param password Oracle credential
     * @return new HTTP Client ready to access eDelivery
     */
    public static CloseableHttpClient getOraClient(String userId, String password) {
        logger.entering(userId);
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);
        config.setCookieSpec(StandardCookieSpec.RELAXED);

        BasicCookieStore cookieStore = new BasicCookieStore();

        HttpClientBuilder builder = HttpClientBuilder.create()
            .setDefaultRequestConfig(config.build())
            .setRetryStrategy(retryHandler())
            .setUserAgent("Wget/1.10")
            .setDefaultCookieStore(cookieStore).useSystemProperties();

        if (userId != null && password != null) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(userId, password.toCharArray()));
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        CloseableHttpClient result = builder.useSystemProperties().build();
        logger.exiting();
        return result;
    }

    /**
     * Return a Executor for http access.
     * @param supportUserName  oracle support username
     * @param supportPassword oracle support password
     * @return Executor
     */

    public static Executor getHttpExecutor(String supportUserName, String supportPassword) {

        String proxyUser = System.getProperty("https.proxyUser");
        String proxyPassword = System.getProperty("https.proxyPassword");
        String proxyHost = System.getProperty("https.proxyHost");
        String proxyPort  = System.getProperty("https.proxyPort");
        Executor executor = Executor.newInstance(getOraClient(supportUserName, supportPassword));


        if (proxyHost != null) {
            HttpHost proxy = new HttpHost("https", proxyHost, Integer.parseInt(proxyPort));
            if (proxyPassword != null) {
                executor.auth(proxy, proxyUser, proxyPassword.toCharArray());
            }

            if (supportUserName != null && supportPassword != null) {
                executor
                    .auth(new HttpHost("https", "login.oracle.com", 443), supportUserName,
                        supportPassword.toCharArray())
                    .auth(new HttpHost("https", Constants.ARU_UPDATES_HOST, 443), supportUserName,
                        supportPassword.toCharArray());
            }
            executor.authPreemptiveProxy(proxy);
        }
        return executor;
    }

    private static HttpRequestRetryStrategy retryHandler() {
        return new DefaultHttpRequestRetryStrategy(10, TimeValue.ofSeconds(2)) {
            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int executionCount,
                                        org.apache.hc.core5.http.protocol.HttpContext context) {
                if (exception instanceof InterruptedIOException) {
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    return false;
                }
                if (exception instanceof SSLException) {
                    return false;
                }
                boolean retriable = !(request instanceof ClassicHttpRequest)
                    || ((ClassicHttpRequest) request).getEntity() == null;
                if (retriable) {
                    long waitTime = executionCount < 5 ? 2 : 10;
                    logger.warning("Connect failed, retrying in {0} seconds, attempts={1} ", waitTime, executionCount);
                }
                return retriable && super.retryRequest(request, exception, executionCount, context);
            }

            @Override
            public TimeValue getRetryInterval(org.apache.hc.core5.http.HttpResponse response,
                                              int execCount,
                                              org.apache.hc.core5.http.protocol.HttpContext context) {
                return execCount < 5 ? TimeValue.ofSeconds(2) : TimeValue.ofSeconds(10);
            }
        };
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
        BasicCookieStore cookieStore = new BasicCookieStore();

        Executor httpExecutor = HttpUtil.getHttpExecutor(username, password);
        httpExecutor.use(cookieStore);


        // Has to do search first, otherwise results in 302
        // MUST use the same httpExecutor to maintain session


        boolean complete = false;
        int count = 0;
        String xmlString = null;

        while (!complete) {
            try {
                httpExecutor
                    .execute(Request.get(Constants.REL_URL).connectTimeout(REQUEST_TIMEOUT)
                        .responseTimeout(REQUEST_TIMEOUT))
                    .returnContent().asString();

                HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody("request_xml", payload)
                    .build();

                xmlString =
                    httpExecutor.execute(Request.post(url).connectTimeout(REQUEST_TIMEOUT)
                        .responseTimeout(REQUEST_TIMEOUT)
                        .body(entity))
                        .returnContent().asString();
                complete = true;
            } catch (IOException ioe) {
                if (++count > 10) {
                    complete = true;
                } else {
                    logger.warning("Network connection failed, retrying in 10 seconds, attempts={0} ", count);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            }
        }
        logger.exiting();
        return parseXml(xmlString);

    }
}
