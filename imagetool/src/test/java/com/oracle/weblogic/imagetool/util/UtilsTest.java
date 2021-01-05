// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class UtilsTest {

    @Test
    void compareVersions() {
        assertEquals(0, Utils.compareVersions("12.2.1.3.0", "12.2.1.3.0"));
        assertTrue(Utils.compareVersions("1.0", "1.1") < 0);
        assertTrue(Utils.compareVersions("1.1", "1.0") > 0);
    }

    @Test
    void isEmptyString() {
        assertTrue(Utils.isEmptyString(""));
    }

    @Test
    void settingProxy() throws IOException {
        String host = "www-proxy.mycompany.com";
        String port = "80";
        String simpleProxy = host + ":" + port;
        String simpleHttp = "http://" + simpleProxy;
        String simpleHttps = "https://" + simpleProxy;

        // proxy value with matching protocol, http for http, and https for https
        Utils.setProxyIfRequired(simpleHttp, simpleHttps, "");
        assertEquals(host, System.getProperty("http.proxyHost"));
        assertEquals(port, System.getProperty("http.proxyPort"));
        assertEquals(host, System.getProperty("https.proxyHost"));
        assertEquals(port, System.getProperty("https.proxyPort"));

        // proxy value with same protocol, http for http, and same for https
        Utils.setProxyIfRequired(simpleHttp, simpleHttp, "");
        assertEquals(host, System.getProperty("http.proxyHost"));
        assertEquals(port, System.getProperty("http.proxyPort"));
        assertEquals(host, System.getProperty("https.proxyHost"));
        assertEquals(port, System.getProperty("https.proxyPort"));

        // proxy value with no protocol
        Utils.setProxyIfRequired(simpleProxy, simpleProxy, "");
        assertEquals(host, System.getProperty("http.proxyHost"));
        assertEquals(port, System.getProperty("http.proxyPort"));
        assertEquals(host, System.getProperty("https.proxyHost"));
        assertEquals(port, System.getProperty("https.proxyPort"));
    }

    @Test
    void passwordResolution() throws Exception {

        assertEquals("pass1",
            Utils.getPasswordFromInputs("pass1", null, null),
            "getPasswordFromInputs did not retrieve the password from a string");

        assertEquals("pass2",
            Utils.getPasswordFromInputs(null,
                Paths.get("./src/test/resources/utilsTest/testPasswordFile.txt"),
                null),
            "getPasswordFromInputs did not retrieve the password from a file");

        assertEquals("pass3",
            Utils.getPasswordFromInputs(null, null, "TEST_ENV_PASSWORD"),
            "getPasswordFromInputs did not retrieve the password from the environment");

        assertEquals("pass2",
            Utils.getPasswordFromInputs(null,
                Paths.get("./src/test/resources/utilsTest/testPasswordFile.txt"),
                "TEST_ENV_PASSWORD"),
            "getPasswordFromInputs did not give preference to the file over the environment");
    }

    @Test
    void oracleHomeFromResponseFile() throws Exception {
        LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
        try {
            Path responseFile = Paths.get("./src/test/resources/utilsTest/responseFile1.txt");
            List<Path> responseFiles = new ArrayList<>();
            responseFiles.add(responseFile);
            DockerfileOptions options = new DockerfileOptions("tests");

            Utils.setOracleHome(responseFiles, options);
            assertEquals("/my/oraclehomeDir", options.oracle_home(),
                "set Oracle Home from response file failed");
        } finally {
            logger.setLevel(oldLevel);
        }
    }

    @Test
    void getPsuVersionFromInventoryOldStyle() throws IOException {
        Path inventoryPath = Paths.get("./src/test/resources/utilsTest/lsinventory.txt");
        String inventoryStr = new String(Files.readAllBytes(inventoryPath));
        assertEquals("12.2.1.4.191220", Utils.getPsuVersion(inventoryStr), "Utils.getPsuVersion is failing");
    }

    @Test
    void getPsuVersionFromInventoryIdStyle() throws IOException {
        Path inventoryPath = Paths.get("./src/test/resources/utilsTest/lsinventoryWithID.txt");
        String inventoryStr = new String(Files.readAllBytes(inventoryPath));
        assertEquals("12.2.1.3.200227", Utils.getPsuVersion(inventoryStr), "Utils.getPsuVersion is failing");
    }
}