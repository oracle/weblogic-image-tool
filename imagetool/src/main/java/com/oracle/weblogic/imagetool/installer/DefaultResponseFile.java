// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.IOException;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class DefaultResponseFile implements ResponseFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(DefaultResponseFile.class);

    private String resource;
    private String filename;

    /**
     * Use one of the default response files provided in the tool.
     * @param resource the resource path to the response file in this tool
     */
    public DefaultResponseFile(String resource) {
        this.resource = resource;
        if (resource.contains("/")) {
            filename = resource.substring(resource.lastIndexOf("/") + 1);
        } else {
            filename = resource;
        }
    }

    @Override
    public String name() {
        return filename;
    }

    @Override
    public void copyFile(String buildContextDir) throws IOException {
        Utils.copyResourceAsFile(resource, buildContextDir, false);
    }
}
