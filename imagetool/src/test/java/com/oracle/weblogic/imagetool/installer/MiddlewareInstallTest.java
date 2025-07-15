// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@ReduceTestLogging(loggerClass = MiddlewareInstall.class)
class MiddlewareInstallTest {
    static Path cacheDir;

    @BeforeAll
    static void setup(@TempDir Path cacheDir) throws IOException {
        MiddlewareInstallTest.cacheDir = cacheDir;

        Path path12214 = ResourceUtils.resourcePath("/dummyInstallers/test-installer.zip");

        Path settingsFileName = cacheDir.resolve("settings.yaml");
        Path installerFile = cacheDir.resolve("installers.yaml");
        Path patchFile = cacheDir.resolve("patches.yaml");
        Files.createFile(settingsFileName);
        Files.createFile(installerFile);
        Files.createFile(patchFile);

        List<String> lines = Arrays.asList(
            "installerSettingsFile: " + installerFile.toAbsolutePath().toString(),
            "patchSettingsFile: " + patchFile.toAbsolutePath().toString(),
            "installerDirectory: " + cacheDir.toAbsolutePath().toString(),
            "patchDirectory: " + cacheDir.toAbsolutePath().toString()
        );
        Files.write(settingsFileName, lines);
        ConfigManager configManager = ConfigManager.getInstance(settingsFileName);
        InstallerMetaData installer2 = new InstallerMetaData("Generic",
            path12214.toString(),
            "12.2.1.4.0", "12.2.1.4.0");

        configManager.addInstaller(InstallerType.WLS, "12.2.1.4.0", installer2);
    }

    @Test
    void copyInstaller(@TempDir Path buildContextDir) throws IOException {
        // Test a simple WLS install type, and copy the files to the build context folder
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0",
            null, null, "docker", null);
        install.copyFiles(buildContextDir.toString());
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
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0", customResponse,
            null, "docker", null);
        install.copyFiles(buildContextDir.toString());
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
