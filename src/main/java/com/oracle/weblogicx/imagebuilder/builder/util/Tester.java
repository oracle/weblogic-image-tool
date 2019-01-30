package com.oracle.weblogicx.imagebuilder.builder.util;

import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_WLS_VERSION;

public class Tester {

    public static void main(String[] args) throws IOException, TransformerException {
        Path DEFAULT_META_PATH = Paths.get(System.getProperty("user.home") + File.separator + "cache" +
                File.separator + ".metadata");
        System.out.println(DEFAULT_META_PATH.toString());
        System.out.println(DEFAULT_META_PATH.toAbsolutePath());

        System.out.println("============");

        SearchResult result = ARUUtil.getPatchDetail(WLSInstallerType.WLS.toString(), DEFAULT_WLS_VERSION, "27342434", "gopi.suryadevara@oracle.com", "omSAI@123");
        if (result.isSuccess()) {
            Document document = result.getResults();
            printDocument(document, System.out);
        } else {
            System.out.println("Error occurred: " + result.getErrorMessage());
        }

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
