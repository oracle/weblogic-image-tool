// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.util.HttpUtil;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test helper class for loading/parsing test resource files.
 */
public class ResourceUtils {

    private static ResourceUtils internal;

    private ResourceUtils() {
        // use instance()
    }

    private static ResourceUtils instance() {
        if (internal == null) {
            internal = new ResourceUtils();
        }
        return internal;
    }

    public static Path resourcePath(String pathOfResource) {
        URL url = instance().getClass().getResource(pathOfResource);
        assertNotNull(url);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException err) {
            throw new IllegalArgumentException("Invalid path to resource: " + pathOfResource);
        }
    }

    public static Document getXmlFromResource(String pathOfResource) throws IOException {
        return HttpUtil.parseXml(Files.readAllBytes(resourcePath(pathOfResource)));
    }
}
