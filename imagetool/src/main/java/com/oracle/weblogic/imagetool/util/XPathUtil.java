// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.StringWriter;
import java.io.Writer;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathUtil {

    private static final LoggingFacade logger = LoggingFactory.getLogger(XPathUtil.class);

    private static XPathFactory factory = null;

    private static synchronized XPathFactory factory() {
        if (factory == null) {
            factory = XPathFactory.newInstance();
        }
        return factory;
    }

    /**
     * Apply XPath and return the results as nodelist.
     *
     * @param node       node
     * @param expression xpath expression
     * @return nodelist result
     * @throws XPathExpressionException when xpath failed
     */
    public static NodeList nodelist(Node node, String expression) throws XPathExpressionException {
        XPath xpath = factory().newXPath();
        return (NodeList) xpath.evaluate(expression, node, XPathConstants.NODESET);
    }

    /**
     * Apply XPath and return the results as string.
     *
     * @param doc        dom document
     * @param expression xpath expression
     * @return string result
     * @throws XPathExpressionException when xpath failed
     */
    public static String string(Document doc, String expression) throws XPathExpressionException {
        XPath xpath = factory().newXPath();
        return (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
    }

    /**
     * Apply XPath and return the results as string.
     *
     * @param doc        node from a nodelist
     * @param expression xpath expression
     * @return string result
     * @throws XPathExpressionException when xpath failed
     */
    public static String string(Node doc, String expression) throws XPathExpressionException {
        XPath xpath = factory().newXPath();
        return (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
    }

    /**
     * Pretty print the document.
     *
     * @param xml dom document
     */
    public static String prettyPrint(Document xml) {
        try {
            TransformerFactory t = TransformerFactory.newInstance();
            t.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            t.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            t.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            t.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Transformer tf = t.newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            Writer out = new StringWriter();
            tf.transform(new DOMSource(xml), new StreamResult(out));
            return out.toString();
        } catch (TransformerException ex) {
            String errMsg = "Failed to print out xml document, probably not a valid document " + ex.getMessage();
            logger.fine(errMsg);
            return errMsg;
        }
    }

}