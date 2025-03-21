// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.ResourceUtils;
import com.oracle.weblogic.imagetool.cli.menu.PackageManagerType;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ReduceTestLogging(loggerClass = MiddlewareInstall.class)
@Tag("unit")
class DockerfileBuilderTest {

    @BeforeAll
    static void setup(@TempDir Path cacheDir) throws IOException {

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

    /**
     * Catch mismatched start/end tags and missing mustache braces.
     * @throws IOException if file read fails for mustache file.
     */
    @Test
    void validateMustacheAliases() throws IOException {
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.4.0",
            null, null, "docker");

        DockerfileOptions dockerfileOptions = new DockerfileOptions("123")
            .setPatchingEnabled()
            .setOPatchPatchingEnabled()
            .setWdtEnabled()
            .setWdtDomainType("WLS")
            .setWdtModels(Arrays.asList("model1.yaml", "model2.yaml"))
            .setPackageInstaller(PackageManagerType.YUM)
            .setWdtInstallerFilename("weblogic-deploy.zip")
            .setMiddlewareInstall(install);

        MustacheFactory mf = new DefaultMustacheFactory(new File("src/main/resources/docker-files"));
        Mustache mustache = mf.compile("Create_Image.mustache");
        mustache.execute(new StringWriter(), dockerfileOptions).flush();
        assertTrue(true);
    }

    @Test
    void setPackageInstaller() {
        DockerfileOptions options = new DockerfileOptions("123").setPackageInstaller(PackageManagerType.YUM);
        assertTrue(options.useYum(), "Failed to set YUM installer");

        options = new DockerfileOptions("123").setPackageInstaller(PackageManagerType.APTGET);
        assertTrue(options.useAptGet(), "Failed to set 'APTGET' installer");

        options = new DockerfileOptions("123").setPackageInstaller(PackageManagerType.ZYPPER);
        assertTrue(options.useZypper(), "Failed to set 'ZYPPER' installer");
    }
}