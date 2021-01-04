// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.cli.menu.PackageManagerType;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class DockerfileBuilderTest {

    /**
     * Catch mismatched start/end tags and missing mustache braces.
     * @throws IOException if file read fails for mustache file.
     */
    @Test
    void validateMustacheAliases() throws IOException {
        MiddlewareInstall install = new MiddlewareInstall(FmwInstallerType.WLS, "12.2.1.3", null);

        DockerfileOptions dockerfileOptions = new DockerfileOptions("123")
            .setPatchingEnabled()
            .setOPatchPatchingEnabled()
            .setWdtEnabled()
            .setWdtDomainType("WLS")
            .setWdtModels(Arrays.asList("model1.yaml", "model2.yaml"))
            .setPackageInstaller(PackageManagerType.YUM)
            .setMiddlewareInstall(install);

        MustacheFactory mf = new DefaultMustacheFactory(new File("src/main/resources/docker-files"));
        Mustache mustache = mf.compile("Create_Image.mustache");
        //mustache.execute(new PrintWriter(System.out), dockerfileOptions).flush();
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