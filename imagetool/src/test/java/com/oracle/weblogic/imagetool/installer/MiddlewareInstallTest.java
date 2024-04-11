// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreTestImpl;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class MiddlewareInstallTest {
    static Path cacheDir;
    static CacheStore cacheStore;
    private static final LoggingFacade commandLogger = LoggingFactory.getLogger(MiddlewareInstall.class);
    private static Level oldLevel;

    @BeforeAll
    static void setup(@TempDir Path cacheDir) throws IOException {
        oldLevel = commandLogger.getLevel();
        commandLogger.setLevel(Level.WARNING);

        MiddlewareInstallTest.cacheDir = cacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);
        cacheStore.addToCache("wls_12.2.1.4.0",
            ResourceUtils.resourcePath("/dummyInstallers/test-installer.zip").toString());
    }

    @AfterAll
    static void tearDown() {
        commandLogger.setLevel(oldLevel);
    }

    @Test
    void copyInstaller(@TempDir Path buildContextDir) throws IOException {
        // Test a simple WLS install type, and copy the files to the build context folder
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0", null, null);
        install.copyFiles(cacheStore, buildContextDir.toString());
        // 2 files should be copied from cache to build context folder
        assertTrue(Files.isRegularFile(buildContextDir.resolve("test-installer.zip")));
        assertTrue(Files.isRegularFile(buildContextDir.resolve("wls.rsp")), "Response file not found");
        // JAR name from inside the zip should be correctly identified
        List<MiddlewareInstallPackage> installers = install.getInstallers();
        assertEquals(1, installers.size());

        MiddlewareInstallPackage pkg = installers.get(0);
        assertTrue(pkg.isZip);
        assertEquals("the-installer.jar", pkg.jarName);
        assertEquals("wls.rsp", pkg.responseFile.name());
        assertInstanceOf(DefaultResponseFile.class, pkg.responseFile);
    }

    @Test
    void customResponseFile(@TempDir Path buildContextDir) throws IOException {
        List<Path> customResponse =
            Collections.singletonList(ResourceUtils.resourcePath("/dummyInstallers/dummyResponse.txt"));
        // Test a simple WLS install type, and copy the files to the build context folder
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0", customResponse, null);
        install.copyFiles(cacheStore, buildContextDir.toString());
        // 2 files should be copied from cache to build context folder
        assertTrue(Files.isRegularFile(buildContextDir.resolve("test-installer.zip")));
        assertTrue(Files.isRegularFile(buildContextDir.resolve("dummyResponse.txt")), "Response file not found");
        // JAR name from inside the zip should be correctly identified
        List<MiddlewareInstallPackage> installers = install.getInstallers();
        assertEquals(1, installers.size());

        MiddlewareInstallPackage pkg = installers.get(0);
        assertTrue(pkg.isZip);
        assertEquals("the-installer.jar", pkg.jarName);
        assertEquals("dummyResponse.txt", pkg.responseFile.name());
        assertInstanceOf(ProvidedResponseFile.class, pkg.responseFile);
    }
}
