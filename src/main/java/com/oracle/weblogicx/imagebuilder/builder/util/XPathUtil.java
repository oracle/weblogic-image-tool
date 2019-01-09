package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XPathUtil {

    public static NodeList applyXPathReturnNodeList(Document doc, String expression) throws XPathExpressionException {

        XPathFactory factory = XPathFactory.newInstance();

        XPath xpath = factory.newXPath();

        NodeList nodeList;

        nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        return nodeList;

    }


    public static String applyXPathReturnString(Document doc, String expression) throws XPathExpressionException {

        XPathFactory factory = XPathFactory.newInstance();

        XPath xpath = factory.newXPath();

        return (String)xpath.evaluate(expression, doc, XPathConstants.NODESET);

    }



}