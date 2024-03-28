// Copyright (c) 2020, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreTestImpl;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstallPackage;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class MiddlewareInstallTest {
    static Path cacheDir;
    static CacheStore cacheStore;
    static final String SOME_VERSION = "12.2.1.4.0";
    static final String installerFilename = "installer_file_122140.zip";
    static final String installerJarname = "installer.jar";

    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;


    @BeforeAll
    static void setup(@TempDir Path tempDir, @TempDir Path tmpCacheDir) throws IOException {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);

        cacheDir = tmpCacheDir;
        cacheStore  = new CacheStoreTestImpl(cacheDir);

        // Create a dummy installer ZIP with a dummy JAR installer inside
        Path zipPath = tempDir.resolve(installerFilename);
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zipPath));
        ZipEntry e = new ZipEntry(installerJarname);
        out.putNextEntry(e);
        byte[] data = "Test".getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();

        // build a fake cache with two dummy installers
        String key1 = "wls_" + SOME_VERSION;
        cacheStore.addToCache(key1, zipPath.toString());
        cacheStore.addToCache("wls_14.1.1.0.0", "/dont/care");
    }

    @AfterAll
    static void tearDown() throws NoSuchFieldException, IllegalAccessException {
        logger.setLevel(oldLevel);
    }

    @Test
    void copyFiles(@TempDir Path buildDir) throws Exception {
        // Verify that the installer does not already exist in the build directory
        Path file = buildDir.resolve(installerFilename);
        assertFalse(Files.exists(file));

        MiddlewareInstall install =
            new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0", null, null);

        // Using the fake cache, resolve the WLS installer for 12.2.1.4.0, and copy the files to the build directory
        install.copyFiles(cacheStore, buildDir.toString());

        MiddlewareInstallPackage pkg = install.getInstallers().get(0);
        assertEquals(installerFilename, pkg.installerFilename());
        assertEquals(installerJarname, pkg.jarName());
        assertTrue(pkg.isZip());
        assertNull(pkg.prereqFile());
        assertNull(pkg.prereqConfigLoc());

        // Verify that the installer was copied to the build directory
        assertTrue(Files.exists(file));
        assertTrue(Files.isRegularFile(file));
    }
}
