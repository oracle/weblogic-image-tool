package com.oracle.weblogicx.imagebuilder.builder.util;


import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class HttpUtil {

    public static Document getXMLContent(String url) throws IOException {

        Document result = Request.Get(url)
            .execute().handleResponse(new ResponseHandler<Document>() {

                public Document handleResponse(final HttpResponse response) throws IOException {
                    StatusLine statusLine = response.getStatusLine();
                    HttpEntity entity = response.getEntity();
                    if (statusLine.getStatusCode() >= 300) {
                        throw new HttpResponseException(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                    }
                    if (entity == null) {
                        throw new ClientProtocolException("Response contains no content");
                    }
                    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
                    try {
                        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                        ContentType contentType = ContentType.getOrDefault(entity);
                        if (!contentType.equals(ContentType.APPLICATION_XML)) {
                            throw new ClientProtocolException("Unexpected content type:" +
                                contentType);
                        }
                        String charset =  contentType.getCharset().toString();
                        if (charset == null) {
                            charset = HTTP.DEFAULT_CONTENT_CHARSET;
                        }
                        return docBuilder.parse(entity.getContent(), charset);
                    } catch (ParserConfigurationException ex) {
                        throw new IllegalStateException(ex);
                    } catch (SAXException ex) {
                        throw new ClientProtocolException("Malformed XML document", ex);
                    }
                }

            });

        return result;

    }

    public static void downloadFile(String url, String destination) throws IOException {
        Request.Get(url)
            .connectTimeout(1000)
            .socketTimeout(1000)
            .execute().saveContent(new File(destination));
    }

}
