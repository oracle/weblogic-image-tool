// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@ReduceTestLogging(loggerClass = CachedFile.class)
@ExtendWith(SystemStubsExtension.class)
class UtilsTest {

    @SystemStub
    private EnvironmentVariables environment;

    @SystemStub
    private SystemProperties overrideProperties;

    @Test
    void firstGreaterThanLast() {
        String thisVersion = "12.2.1.3.0";
        String thatVersion = "12.2.1.2.0";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "0.7.4";
        thatVersion = "0.7.3";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "0.8";
        thatVersion = "0.7.4";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "1.2.3";
        thatVersion = "1.2";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "13.9.4.2.10";
        thatVersion = "13.9.4.2.9";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "1.2.3";
        thatVersion = "1.2.3-SNAPSHOT";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "1.2.3-BETA1";
        thatVersion = "1.2.3-ALPHA3";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);

        thisVersion = "1.2.3-SNAPSHOT";
        thatVersion = "1.2.2";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) > 0,
            "Expected this " + thisVersion + " to be newer than that " + thatVersion);
    }

    @Test
    void secondGreaterThanFirst() {
        String thisVersion = "12.2.1.3.0";
        String thatVersion = "12.2.1.4.0";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "0.7.3";
        thatVersion = "0.7.4";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "0.7.4";
        thatVersion = "1";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "1.2";
        thatVersion = "1.2.3";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "1.2.3-SNAPSHOT";
        thatVersion = "1.2.3";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "1.2.3-ALPHA4";
        thatVersion = "1.2.3-ALPHA5";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);

        thisVersion = "1.2.1";
        thatVersion = "1.2.2-SNAPSHOT";
        assertTrue(Utils.compareVersions(thisVersion, thatVersion) < 0,
            "Expected that " + thatVersion + " to be newer than this " + thisVersion);
    }

    @Test
    void versionsShouldBeEqual() {
        String thisVersion = "12.2.1.3.0";
        String thatVersion = "12.2.1.3.0";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);

        thisVersion = "0.7.4";
        thatVersion = "0.7.4";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);

        thisVersion = "1";
        thatVersion = "1";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);

        thisVersion = "1.2.3-ALPHA1";
        thatVersion = "1.2.3-ALPHA1";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);

        thisVersion = "1.2.3.4.5.6.7.8.9";
        thatVersion = "1.2.3.4.5.6.7.8.9";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);

        thisVersion = "0.7.4-SNAPSHOT";
        thatVersion = "0.7.4-SNAPSHOT";
        assertEquals(0, Utils.compareVersions(thisVersion, thatVersion),
            "Expected server " + thatVersion + " to be the same as client " + thisVersion);
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
        Path responseFile = Paths.get("./src/test/resources/utilsTest/responseFile1.txt");
        List<Path> responseFiles = new ArrayList<>();
        responseFiles.add(responseFile);
        DockerfileOptions options = new DockerfileOptions("tests");

        Utils.setOracleHome(responseFiles, options);
        assertEquals("/my/oraclehomeDir", options.oracle_home(),
            "set Oracle Home from response file failed");
    }

    @Test
    @DisplayName("Default working directory to User's home dir")
    void getBuildWorkingDir1() throws IOException {
        String expected = System.getProperty("user.home");
        assertEquals(expected, Utils.getBuildWorkingDir());
    }

    @Test
    @DisplayName("Override working directory with system property")
    void getBuildWorkingDir2(@TempDir Path tempDir) throws IOException {
        // provide existing directory as input to WLSIMG_BLDDIR should succeed
        String expected = tempDir.toString();
        overrideProperties.set("WLSIMG_BLDDIR", expected);
        assertEquals(expected, Utils.getBuildWorkingDir());

        // Create a read-only directory as input for WLSIMG_BLDDIR
        Path unwritableDir = tempDir.resolve("unwritable");
        Files.createDirectory(unwritableDir);
        if (!unwritableDir.toFile().setReadOnly()) {
            throw new IOException("Unable to mark test directory as read-only");
        }
        // read-only directory as input for working directory should throw an exception
        overrideProperties.set("WLSIMG_BLDDIR", unwritableDir.toString());
        assertThrows(IOException.class, Utils::getBuildWorkingDir);
    }

    @Test
    @DisplayName("Override working directory with invalid directory")
    void getBuildWorkingDir3() {
        String expected = "/this/does/not/exist";
        overrideProperties.set("WLSIMG_BLDDIR", expected);
        assertThrows(IOException.class, Utils::getBuildWorkingDir);
    }

    @Test
    @DisplayName("Override working directory with invalid file")
    void getBuildWorkingDir4(@TempDir Path tempDir) throws IOException {
        Path tempFile = tempDir.resolve("getBuildWorkingDir4.txt");
        List<String> lines = Arrays.asList("a", "b");
        Files.write(tempFile, lines);

        String expected = tempFile.toString();
        overrideProperties.set("WLSIMG_BLDDIR", expected);
        // The override for WLSIMG_BUILDDIR must be a directory, NOT a file
        assertThrows(IOException.class, Utils::getBuildWorkingDir);
    }

    @Test
    void findProxyUrlWithCommandLine() {
        // Always default to the proxy set by the user on the tool's command line
        String expected = "http://some.proxy-host.com";
        assertEquals(expected, Utils.findProxyUrl(expected, "http"));
        assertEquals(expected, Utils.findProxyUrl(expected, "https"));
    }

    /**
     * If not specified by the user, the environment variable value should be returned
     * for http_proxy and https_proxy.
     */
    @Test
    void findProxyUrlWithEnvironment() {
        // No ENV variable set (filed issue on SystemStub)
        environment.set("http_proxy", null);
        environment.set("HTTP_PROXY", null);
        assertNull(Utils.findProxyUrl("", "http"));

        String expected = "http://env.proxy-host.com";
        // ENV for http_proxy and https_proxy are set
        environment.set("http_proxy", expected);
        environment.set("https_proxy", expected);
        assertEquals(expected, Utils.findProxyUrl("", "http"));
        assertEquals(expected, Utils.findProxyUrl("", "https"));
    }

    @Test
    void findProxyUrlForNoProxy() {
        // No ENV variable set (filed issue on SystemStub)
        environment.set("no_proxy", null);
        environment.set("NO_PROXY", null);
        overrideProperties.set("http.nonProxyHosts", "");
        assertNull(Utils.findProxyUrl("", "none"));

        String expected = ".host.com,.anotherhost.com,.and.another.com";
        // | (bar) should be replaced by , (comma) for http.nonProxyHosts system property
        String withBars = expected.replace(",", "|");
        overrideProperties.set("http.nonProxyHosts", withBars);
        assertEquals(expected, Utils.findProxyUrl("", "none"));

        // ENV for no_proxy is set
        environment.set("no_proxy", expected);
        assertEquals(expected, Utils.findProxyUrl("", "none"));
    }

    @Test
    void mustacheFactory(@TempDir Path outputDir) throws IOException {
        String oracleHome = "/Oracle/Middleware_12214";
        DockerfileOptions options = new DockerfileOptions("111");
        options.setOracleHome(oracleHome);
        String output = Utils.writeDockerfile(outputDir.toString() + File.separator + "Dockerfile",
            "mustache-writer-test.mustache", options, true);
        assertEquals(oracleHome, output);
        assertEquals(1, Objects.requireNonNull(outputDir.toFile().list()).length);
    }
}