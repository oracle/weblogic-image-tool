package com.oracle.weblogicx.imagebuilder.builder.util;

import com.oracle.weblogicx.imagebuilder.builder.api.model.User;
import com.oracle.weblogicx.imagebuilder.builder.api.model.UserSession;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static com.oracle.weblogicx.imagebuilder.builder.impl.service.UserServiceImpl.USER_SERVICE;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.REL_URL;

public class HttpUtil {

    /**
     * Return the xml result of a GET from the url
     *
     * @param url         url of the aru server
     * @param userSession userSession with preconfigured user details
     * @return xml dom document
     * @throws IOException when it fails to access the url
     */
    public static Document getXMLContent(String url, UserSession userSession) throws IOException {
        Executor httpExecutor = getOraHttpExecutor(userSession);
        return getXMLContent(url, httpExecutor);
    }

    /**
     * Return the xml result of a GET from the url
     *
     * @param url      url of the aru server
     * @param username userid for support account
     * @param password password for support account
     * @return xml dom document
     * @throws IOException when it fails to access the url
     */
    public static Document getXMLContent(String url, String username, String password) throws IOException {
        //Executor httpExecutor = getOraHttpExecutor(username, password);
        Executor httpExecutor = getOraHttpExecutor(USER_SERVICE.getUserSession(User.newUser(username, password)));
        return getXMLContent(url, httpExecutor);
    }

    /**
     * Return the xml result of a GET from the url
     *
     * @param url          url of the aru server
     * @param httpExecutor configured Executor object
     * @return xml dom document
     * @throws IOException when it fails to access the url
     */
    private static Document getXMLContent(String url, Executor httpExecutor) throws IOException {
        String xmlString =
            httpExecutor.execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000)).returnContent()
                .asString();
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document doc = docBuilder.parse(is);
            //  XPathUtil.prettyPrint(doc);
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        } catch (Exception g) {
            throw new IllegalStateException(g);
        }
    }

    private static Executor getOraHttpExecutor(UserSession userSession) {
        Executor httpExecutor;
        if (userSession.isUserValidated()) {
            httpExecutor = Executor.newInstance(userSession.getOraClient());
        } else {
            //TODO: throw exception if not already validated.
            User user = userSession.getUser();
            httpExecutor = Executor.newInstance(userSession.getOraClient())
                .auth(user.getEmail(), String.valueOf(user.getPassword()));
        }
        return httpExecutor;
    }

    private static Executor getOraHttpExecutor(String username, String password) {
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);

        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
        cc.setDomain("edelivery.oracle.com");
        cookieStore.addCookie(cc);

        CloseableHttpClient client =
            HttpClientBuilder.create().setDefaultRequestConfig(config.build()).useSystemProperties().build();

        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
        httpExecutor.use(cookieStore);
        return httpExecutor;
    }

    /**
     * Downlod a file from the url
     *
     * @param url      url of the aru server
     * @param fileName full path to save the file
     * @param username userid for support account
     * @param password password for support account
     * @throws IOException when it fails to access the url
     */

    public static void downloadFile(String url, String fileName, String username, String password) throws IOException {
        //        Executor httpExecutor = getOraHttpExecutor(username, password);
        //        httpExecutor.execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
        //            .saveContent(new File(fileName));
        downloadFile(url, fileName, USER_SERVICE.getUserSession(User.newUser(username, password)));
    }

    /**
     * Downlod a file from the url
     *
     * @param url         url of the aru server
     * @param fileName    full path to save the file
     * @param userSession object with httpclient and required details for this user
     * @throws IOException when it fails to access the url
     */
    public static void downloadFile(String url, String fileName, UserSession userSession) throws IOException {
        Executor httpExecutor = getOraHttpExecutor(userSession);
        httpExecutor.execute(Request.Get(url).connectTimeout(30000).socketTimeout(30000))
            .saveContent(new File(fileName));
    }

    /**
     * Check conflicts - probably need to
     *
     * @param url  url for conflict checker api
     * @param payload payload containing patches to check for conflicts
     * @param username user name for support
     * @param password password for support
     * @return
     * @throws IOException
     */

    public static Document checkConflicts(String url, String payload, String username, String password)
        throws IOException {
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);
        config.setRedirectsEnabled(true);

        CookieStore cookieStore = new BasicCookieStore();

        CloseableHttpClient client =
            HttpClientBuilder.create().setDefaultRequestConfig(config.build()).useSystemProperties().build();

        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
        httpExecutor.use(cookieStore);

        // Has to do search first, otherwise results in 302
        getXMLContent(REL_URL, httpExecutor);

        HttpEntity entity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addPart(FormBodyPartBuilder.create("request_xml", new StringBody(payload, ContentType.TEXT_PLAIN)).build())
            .build();

        String xmlString =
            httpExecutor.execute(Request.Post(url).connectTimeout(30000).socketTimeout(30000).body(entity))
                .returnContent().asString();

        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document doc = docBuilder.parse(is);
            //  XPathUtil.prettyPrint(doc);
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        } catch (Exception g) {
            throw new IllegalStateException(g);
        }

    }


}
