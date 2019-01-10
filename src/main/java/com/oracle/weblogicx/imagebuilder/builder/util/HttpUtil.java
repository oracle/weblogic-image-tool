package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HttpUtil {


    public static Document getXMLContent(String url, String username, String password) throws IOException {

        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);


        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
        cc.setDomain("edelivery.oracle.com");
        cookieStore.addCookie(cc);

        CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(config.build())
            .useSystemProperties()
            .build();

        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
        httpExecutor.use(cookieStore);

        String xmlString = httpExecutor.execute(Request.Get(url)
            .connectTimeout(30000)
            .socketTimeout(30000)).returnContent().asString();

        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            return docBuilder.parse(is);
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException(ex);
        } catch (SAXException ex) {
            throw new ClientProtocolException("Malformed XML document", ex);
        }

    }

    public static void downloadFile(String url, String destination, String username, String password) throws
        IOException {
        RequestConfig.Builder config = RequestConfig.custom();
        config.setCircularRedirectsAllowed(true);
        //        config.setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY);


        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cc = new BasicClientCookie("oraclelicense", "a");
        cc.setDomain("edelivery.oracle.com");
        cookieStore.addCookie(cc);

        CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(config.build())
            .useSystemProperties()
            .build();

        Executor httpExecutor = Executor.newInstance(client).auth(username, password);
        httpExecutor.use(cookieStore);

        httpExecutor.execute(Request.Get(url)
            .connectTimeout(30000)
            .socketTimeout(30000)).saveContent(new File(destination));

    }


    public static void main(String args[]) throws IOException {


    }
}
