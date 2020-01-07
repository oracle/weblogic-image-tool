// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class ProvidedResponseFile implements ResponseFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(ProvidedResponseFile.class);

    private String filename;
    private String filePath;

    /**
     * Use one of the default response files provided in the tool.
     * @param path the resource path to the response file in this tool
     */
    public ProvidedResponseFile(String path) {
        filePath = path;
        if (path.contains("/")) {
            filename = path.substring(path.lastIndexOf("/") + 1);
        } else {
            filename = path;
        }
    }


    @Override
    public String name() {
        return filename;
    }

    @Override
    public void copyFile(String buildContextDir) throws IOException {
        if (filePath != null && Files.isRegularFile(Paths.get(filePath))) {
            logger.fine("IMG-0005", filePath);
            Path target = Paths.get(buildContextDir, filename);
            Files.copy(Paths.get(filePath), target);
        }
        //TODO what if its null or not a good file?
    }
}
