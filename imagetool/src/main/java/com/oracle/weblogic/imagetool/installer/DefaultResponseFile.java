// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class DefaultResponseFile implements ResponseFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(DefaultResponseFile.class);

    private static final String R_WLS = "WebLogic Server";
    private static final String R_FMW = "Fusion Middleware Infrastructure";
    private static final String R_SOA = "SOA Suite";
    private static final String R_OSB = "Service Bus";
    private static final String R_OHS = "Standalone HTTP Server (Managed independently of WebLogic server)";
    private static final String R_OHS_WLS = "Collocated HTTP Server (Managed through WebLogic server)";
    private static final String R_IDM = "";
    private static final String R_OAM =
        "Collocated Oracle Identity and Access Manager (Managed through WebLogic server)";
    private static final String R_OUD = "Installation for Oracle Unified Directory";
    private static final String R_WCC = "WebCenter Content";
    private static final String R_WCP = "WebCenter Portal";
    private static final String R_WCS = "WebCenter Sites";

    private final String installTypeResponse;
    private final String filename;

    /**
     * Use the default response file for FMW installers.
     * @param installerType the installer type with which this response file will be used
     */
    public DefaultResponseFile(InstallerType installerType, FmwInstallerType fmwInstallerType) {
        filename = installerType.toString() + ".rsp";
        installTypeResponse = getInstallTypeResponse(installerType, fmwInstallerType);
    }

    private static String getInstallTypeResponse(InstallerType installerType, FmwInstallerType fmwInstallerType) {
        String response;

        switch (installerType) {
            case FMW:
                response = R_FMW;
                break;
            case SOA:
                response = R_SOA;
                break;
            case OSB:
                response = R_OSB;
                break;
            case OHS:
                if (FmwInstallerType.OHS == fmwInstallerType) {
                    response = R_OHS;
                } else {
                    response = R_OHS_WLS;
                }
                break;
            case IDM:
                response = R_IDM;
                break;
            case OAM:
                response = R_OAM;
                break;
            case OUD:
                response = R_OUD;
                break;
            case WCC:
                response = R_WCC;
                break;
            case WCP:
                response = R_WCP;
                break;
            case WCS:
                response = R_WCS;
                break;
            case WLS:
            default:
                response = R_WLS;
                break;
        }


        return response;
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
