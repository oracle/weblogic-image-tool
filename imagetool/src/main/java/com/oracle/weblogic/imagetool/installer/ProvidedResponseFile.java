// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
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

    private Path responseFileSource;

    /**
     * Use one of the default response files provided in the tool.
     * @param source the resource path to the response file in this tool
     */
    public ProvidedResponseFile(Path source) {
        responseFileSource = source;
    }

    @Override
    public String name() {
        return responseFileSource.getFileName().toString();
    }

    @Override
    public void copyFile(String buildContextDir) throws IOException {
        if (responseFileSource != null && Files.isRegularFile(responseFileSource)) {
            logger.fine("IMG-0005", responseFileSource);
            Path target = Paths.get(buildContextDir, name());
            Files.copy(responseFileSource, target);
        }
    }
}
