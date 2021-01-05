// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
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
    private String filename = null;

    /**
     * Use one of the default response files provided in the tool.
     * @param source the resource path to the response file in this tool
     */
    public ProvidedResponseFile(Path source) {
        responseFileSource = source;
        if (responseFileSource != null && Files.isRegularFile(responseFileSource)) {
            filename = responseFileSource.getFileName().toString();
        }
    }

    @Override
    public String name() {
        return filename;
    }

    @Override
    public void copyFile(String buildContextDir) throws IOException {
        if (name() != null) {
            logger.fine("IMG-0005", responseFileSource);
            Path target = Paths.get(buildContextDir, name());
            if (Files.exists(target)) {
                int idx = 1;
                String trialName = filename + idx;
                target = Paths.get(buildContextDir, trialName);
                while (Files.exists(target)) {
                    trialName = filename + ++idx;
                    target = Paths.get(buildContextDir, trialName);
                }
                logger.fine("copying installer response file {0} as {1}", filename, trialName);
                filename = trialName;
            }
            Files.copy(responseFileSource, target);
        }
    }
}
