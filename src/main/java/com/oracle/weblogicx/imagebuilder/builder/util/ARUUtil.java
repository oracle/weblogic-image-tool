package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.oracle.weblogicx.imagebuilder.builder.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.*;

public class ARUUtil {

    /**
     * Return All WLS releases information
     *
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     */

    public static Document getAllWLSReleases(String userId, String password) throws IOException {
        return getAllReleases("wls", userId, password);
    }

    /**
     * Return release number of a WLS release by version
     *
     * @param version wls version 12.2.1.3.0 etc ...
     * @param userId  user id for support account
     * @param password password for support account
     * @return release number or empty string if not found
     * @throws IOException when failed to access the aru api
     */
    private static String getWLSReleaseNumber(String version, String userId, String password) throws
        IOException {
        return getReleaseNumber("wls", version, userId, password);
    }

    /**
     * Return release number of a FMW release by version
     *
     * @param version wls version 12.2.1.3.0 etc ...
     * @param userId  user id for support account
     * @param password password for support account
     * @return release number or empty string if not found
     * @throws IOException when failed to access the aru api
     */
    private static String getFMWReleaseNumber(String version, String userId, String password) throws
        IOException {
        return getReleaseNumber("fmw", version, userId, password);
    }


    /**
     * Return All FMW releases information
     *
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     */
    public static void getAllFMWReleases(String userId, String password) throws IOException {
        getAllReleases("fmw", userId, password);
    }

    /**
     * Download the latest WLS patches(PSU) for the release
     *
     * @param version release number
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     * @return String bug number
     */
    public static String getLatestWLSPSU(String version, String userId, String password) throws IOException {
        String releaseNumber = getReleaseNumber("wls", version, userId, password);
        return getLatestPSU("wls", releaseNumber, userId, password);
    }

    /**
     * Download the latest FMW patches(PSU) for the release
     *
     * @param version version number 12.2.1.3.0
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     * @return String bug number
     */
    public static String getLatestFMWPSU(String version, String userId, String password) throws IOException {
        String releaseNumber = getReleaseNumber("wls", version, userId, password);
        return getLatestPSU("fmw", releaseNumber, userId, password);
    }

    /**
     * Download a list of WLS patches
     *
     * @param patches  A list of patches number
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     */
    public static  List<String>  getWLSPatches(List<String> patches, String userId, String password) throws
        IOException {
        List<String> results = new ArrayList<>();
        for (String patch : patches) {
            String rs = getPatch("wls", patch, userId, password);
            if (rs != null) {
                results.add(rs);
            }
        }
        return results;
    }

    /**
     * Download a list of FMW patches
     *
     * @param patches  A list of patches number
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     */
    public static List<String> getFMWPatches(String category, List<String> patches, String userId, String password)
        throws
        IOException {
        List<String> results = new ArrayList<>();
        for (String patch : patches) {
            String rs = getPatch("fmw", patch, userId, password);
            if (rs != null) {
                results.add(rs);
            }
        }
        return results;
    }

    /**
     *
     * @param patches  A list of patches number
     * @param category
     * @param version
     * @param userId userid for support account
     * @param password password for support account
     * @throws IOException  when failed to access the aru api
     */

    public static  void validatePatches(List<String> patches, String category, String version, String userId, String
        password) throws IOException {

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

    private static  Document getAllReleases(String category, String userId, String password) throws IOException {

        //HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}'
       // "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases" -o allarus.xml)

        Document allReleases = HttpUtil.getXMLContent(REL_URL, userId, password);

        try {

            String expression;

            if ("wls".equalsIgnoreCase(category)) {
                expression = "/results/release[starts-with(text(), 'Oracle WebLogic Server')]";
            } else {
                expression = "/results/release[starts-with(text(), 'Fusion Middleware Upgrade')]";
            }
            NodeList nodeList = XPathUtil.applyXPathReturnNodeList(allReleases, expression);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element element = doc.createElement("results");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i);
                Node copyNode = doc.importNode(n, true);

                if (n instanceof Element )
                    element.appendChild(copyNode);
            }

            doc.appendChild(element);
            //XPathUtil.prettyPrint(doc);

            return doc;

        } catch (XPathExpressionException | ParserConfigurationException xpe) {
            throw new IOException(xpe);
        }


    }

    private static String getLatestPSU(String category, String release, String userId, String password) throws
        IOException {

        String expression;
        if ("wls".equalsIgnoreCase(category))
            expression = String.format(LATEST_PSU_URL, WLS_PROD_ID, release);
        else
            expression = String.format(LATEST_PSU_URL, FMW_PROD_ID, release);

        Document allPatches = HttpUtil.getXMLContent(expression, userId, password);
        savePatch(allPatches, userId, password);
    }

    private static String getPatch(String category, String patchNumber, String userId, String password) throws
        IOException {

        //        HTTP_STATUS=$(curl -v -w "%{http_code}" -b cookies.txt -L --header 'Authorization: Basic ${basicauth}' "https://updates.oracle.com/Orion/Services/search?product=15991&release=$releaseid&include_prereqs=true" -o latestpsu.xml)

        String url;
        if ("wls".equalsIgnoreCase(category))
            url = String.format(PATCH_SEARCH_URL, WLS_PROD_ID, patchNumber);
        else
            url = String.format(PATCH_SEARCH_URL, FMW_PROD_ID, patchNumber);

        Document allPatches = HttpUtil.getXMLContent(url, userId, password);

        return savePatch(allPatches, userId, password);
    }

    private static String savePatch(Document allPatches, String userId, String password) throws IOException {
        try {

            // TODO: needs to make sure there is one and some filtering if not sorting

            String downLoadLink = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/text())");

            String downLoadHost = XPathUtil.applyXPathReturnString(allPatches, "string"
                + "(/results/patch[1]/files/file/download_url/@host)");

            String bugName  = XPathUtil.applyXPathReturnString(allPatches, "/results/patch[1]/name");


            int index = downLoadLink.indexOf("patch_file=");

            if (index > 0) {
                String fileName = META_RESOLVER.getCacheDir() + File.separator + downLoadLink.substring(
                        index+"patch_file=".length());
                // this hasMatchingKeyValue is to make sure that the file value is same as the intended location.
                // cache dir can be changed
                if (!META_RESOLVER.hasMatchingKeyValue(bugName, fileName) || !new File(fileName).exists()) {
                    HttpUtil.downloadFile(downLoadHost+downLoadLink, fileName, userId, password);
                    META_RESOLVER.addToCache(bugName, fileName);
                } else {
                    System.out.println(String.format("patch %s already downloaded for bug %s", fileName, bugName));
                }
                return bugName;
            }

        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }

        return null;
    }

    private static String getReleaseNumber(String category, String version, String userId, String password) throws
        IOException {
        Document allReleases = getAllReleases(category, userId, password);

        String expression = String.format("string(/results/release[@name = '%s']/@id)", version);
        try {
            return XPathUtil.applyXPathReturnString(allReleases, expression);
        } catch (XPathExpressionException xpe) {
            throw new IOException(xpe);
        }


    }

    public static boolean checkCredentials(String userId, String password) {
        boolean retVal = true;
        try {
            HttpUtil.getXMLContent(ARU_LANG_URL, userId, password);
        } catch (IOException e) {
            Throwable cause = (e.getCause() == null)? e : e.getCause();
            if (cause.getClass().isAssignableFrom(HttpResponseException.class) &&
                ((HttpResponseException) cause).getStatusCode() == HttpStatus.SC_UNAUTHORIZED ) {
                retVal = false;
            }
        }
        return retVal;
    }

    public static void main(String args[]) throws Exception {
        ARUUtil.getLatestWLSPSU("12.2.1.3.0","johnny.shum@oracle.com", "iJCPiUah7jdmLk1E");
    }


}

