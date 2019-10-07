// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.api.model.DomainType;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DockerfileBuilderTest {

    /**
     * Catch mismatched start/end tags and missing mustache braces.
     * @throws IOException if file read fails for mustache file.
     */
    @Test
    public void validateMustacheAliases() throws IOException {
        DockerfileOptions dockerfileOptions = new DockerfileOptions()
            .setPatchingEnabled()
            .setOPatchPatchingEnabled()
            .setWdtEnabled()
            .setWdtDomainType(DomainType.WLS)
            .setWdtModels(Arrays.asList("model1.yaml", "model2.yaml"))
            .setPackageInstaller(Constants.YUM);

        MustacheFactory mf = new DefaultMustacheFactory(new File("src/main/resources/docker-files"));
        Mustache mustache = mf.compile("Create_Image.mustache");
        //mustache.execute(new PrintWriter(System.out), dockerfileOptions).flush();
        mustache.execute(new StringWriter(), dockerfileOptions).flush();
        assertTrue(true);
    }

    @Test
    public void setPackageInstaller() {
        DockerfileOptions options = new DockerfileOptions().setPackageInstaller(Constants.YUM);
        assertTrue("Failed to set YUM installer", options.useYum);

        options = new DockerfileOptions().setPackageInstaller(Constants.APTGET);
        assertTrue("Failed to set 'APTGET' installer", options.useAptGet);

        options = new DockerfileOptions().setPackageInstaller(Constants.ZYPPER);
        assertTrue("Failed to set 'ZYPPER' installer", options.useZypper);
    }
}