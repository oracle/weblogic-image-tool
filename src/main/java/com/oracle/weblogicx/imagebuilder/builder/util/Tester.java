package com.oracle.weblogicx.imagebuilder.builder.util;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;

public class Tester {

    public static void main(String[] args) throws IOException, TransformerException, URISyntaxException {

        //System.out.println(Paths.get("https://updates.oracle.com/Orion/Services/download/p28186730_139400_Generic.zip?aru=22310944&patch_file=p28186730_139400_Generic.zip").getFileName());
        //System.out.println(new URL("https://updates.oracle.com/Orion/Services/download/p28186730_139400_Generic.zip?aru=22310944&patch_file=p28186730_139400_Generic.zip").getPath());

        String fileName = new URL("https://updates.oracle.com/Orion/Services/download/p28186730_139400_Generic.zip?aru=22310944&patch_file=p28186730_139400_Generic.zip").getPath();
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        System.out.println(fileName);

        String fileName2 = new URL("ftp://abc/some/file").getPath();
        fileName2 = fileName2.substring(fileName2.lastIndexOf('/') + 1);
        System.out.println(fileName2);

//        Path DEFAULT_META_PATH = Paths.get(System.getProperty("user.home") + File.separator + "cache" +
//                File.separator + ".metadata");
//        System.out.println(DEFAULT_META_PATH.toString());
//        System.out.println(DEFAULT_META_PATH.toAbsolutePath());
//
//        System.out.println("============");
//
//        SearchResult result = ARUUtil.getPatchDetail(WLSInstallerType.WLS.toString(), DEFAULT_WLS_VERSION, "27342434", "gopi.suryadevara@oracle.com", "omSAI@123");
//        if (result.isSuccess()) {
//            Document document = result.getResults();
//            printDocument(document, System.out);
//        } else {
//            System.out.println("Error occurred: " + result.getErrorMessage());
//        }

    }

    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

}
