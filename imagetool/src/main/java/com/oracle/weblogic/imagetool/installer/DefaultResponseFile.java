// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class DefaultResponseFile implements ResponseFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(DefaultResponseFile.class);

    private final String installTypeResponse;
    private final String filename;

    /**
     * Use the default response file for FMW installers.
     * @param installerType the installer type with which this response file will be used
     */
    public DefaultResponseFile(InstallerType installerType) {
        filename = installerType.toString() + ".rsp";
        installTypeResponse = installerType.getInstallTypeResponse();
    }

    /**
     * Name for the response file FILE.
     * @return filename to use
     */
    @Override
    public String name() {
        return filename;
    }

    @Override
    public void copyFile(String buildContextDir) throws IOException {
        logger.entering(buildContextDir, filename, installTypeResponse);
        MustacheFactory mf = new DefaultMustacheFactory("response-files");
        Mustache mustache = mf.compile("default-response.mustache");
        mustache.execute(new FileWriter(buildContextDir + File.separator + filename), this).flush();
        logger.exiting();
    }

    /**
     * Get the INPUT_TYPE for the silent install response file.
     * Used by response file Mustache template.
     * @return the value for the response file's input type field
     */
    @SuppressWarnings("unused")
    public String installType() {
        return installTypeResponse;
    }
}
