package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ARUUtil {


    public void getAllWLSReleases(String userId, String password) throws IOException {
        getAllReleases("wls", userId, password);
    }

    public void getLatestWLSPatches(String release, String userId, String password) throws IOException {
        getLatestPSU("wls", release, userId, password);
    }

    public void getPatches(String category, List<String> patches, String userId, String password) throws IOException {
        for (String patch : patches)
            getPatch("wls", patch, userId, password);
    }

    public void validatePatches(List<String> patches, String category, String version) {

        // TODO

        // find the release number first based on the version
        // build the xml

//        <conflict_check_request>
//  <platform>912</platform>
//  <target_patch_list>
//    <installed_patch/>
//  </target_patch_list>
//  <candidate_patch_list>
//    <patch_group rel_id="80111060" language_id="0">7044721</patch_group>
//    <patch_group rel_id="80111060" language_id="0">7156923</patch_group>
//    <patch_group rel_id="80111060" language_id="0">7210195</patch_group>
//    <patch_group rel_id="80111060" language_id="0">7256747</patch_group>
//  </candidate_patch_list>
//</conflict_check_request>

//   Run against POST  /Orion/Services/conflict_checks

    }

    private Document getAllReleases(String category, String userId, String password) throws IOException {

        //HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}'
       // "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases" -o allarus.xml)

        Document allReleases = HttpUtil.getXMLContent("https://updates.oracle"
            + ".com/Orion/Services/metadata?table=aru_releases", userId, password);

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

    private void getLatestPSU(String category, String release, String userId, String password) throws IOException {

//        HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}' "https://updates.oracle.com/Orion/Services/search?product=15991&release=$releaseid&include_prereqs=true" -o latestpsu.xml)


        Document allPatches = HttpUtil.getXMLContent("https://updates.oracle"
            + ".com/Orion/Services/search?product=15991&release=" + release, userId, password);

        savepatch(allPatches, userId, password);
    }

    private void getPatch(String category, String patchNumber, String userId, String password) throws IOException {

        //        HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}' "https://updates.oracle.com/Orion/Services/search?product=15991&release=$releaseid&include_prereqs=true" -o latestpsu.xml)


        Document allPatches = HttpUtil.getXMLContent("https://updates.oracle"
            + ".com/Orion/Services/search?product=15991&bug=" + patchNumber, userId, password);

        savepatch(allPatches, userId, password);



    }

    private void savepatch(Document allPatches, String userId, String password) throws IOException {
        try {

            // TODO: needs to make sure there is one and some filtering if not sorting

            String downLoadLink = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/text())");

            String doloadHost = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/@host)");

            String bugname  = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/name");

            // TODO find the download location

            String fileName = bugname + ".zip";

            HttpUtil.downloadFile(doloadHost+downLoadLink, fileName, userId, password);

            // TODO need method to update the cache data table ?

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

    }



}

