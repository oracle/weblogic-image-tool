package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ARUUtil {


    public void getAllWLSReleases(String userId, String password) throws IOException {
        getAllReleases("wls", userId, password);
    }

    public void getLatestWLSPatches(String version, String userId, String password) throws IOException {
        getLatestReleases("wls", version, userId, password);
    }

    private Document getAllReleases(String category, String userId, String password) throws IOException {

        //HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}'
       // "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases" -o allarus.xml)

        Document allReleases = HttpUtil.getXMLContent("https://updates.oracle"
            + ".com/Orion/Services/metadata?table=aru_releases");

        try {

            String expression;

            if ("wls".equalsIgnoreCase("wls")) {
                expression = "/results/release[starts-with(text(), 'Oracle WebLogic Server')]";
            } else {
                expression = "";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allReleases, expression);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element element = doc.createElement("results");
            doc.appendChild(element);

            for (int i = 0; i < nodeList.getLength(); i++) {
                element.appendChild(nodeList.item(0));
            }


            return doc;

        } catch (XPathExpressionException | ParserConfigurationException xpe) {
            throw new IOException(xpe);
        }


    }

    private void getLatestReleases(String category, String version, String userId, String password) throws IOException {

//        HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}' "https://updates.oracle.com/Orion/Services/search?product=15991&release=$releaseid&include_prereqs=true" -o latestpsu.xml)


        Document allPatches = HttpUtil.getXMLContent("https://updates.oracle"
            + ".com/Orion/Services/search?product=15991&release=" + version);

        try {
            String downLoadLink = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/text())");

            String doloadHost = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/@host)");


            HttpUtil.downloadFile(doloadHost+downLoadLink, "todo");

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }



    }

}

