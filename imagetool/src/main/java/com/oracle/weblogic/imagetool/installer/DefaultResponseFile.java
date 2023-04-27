// Copyright (c) 2019, 2023, Oracle and/or its affiliates.
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
    private static final String R_B2B = "B2B";
    private static final String R_MFT = "";
    private static final String R_IDM =
        "Standalone Oracle Identity and Access Manager(Managed independently of WebLogic server)";
    private static final String R_IDM_WLS =
        "Collocated Oracle Identity and Access Manager (Managed through WebLogic server)";
    private static final String R_OAM =
        "Collocated Oracle Identity and Access Manager (Managed through WebLogic server)";
    private static final String R_OHS = "Standalone HTTP Server (Managed independently of WebLogic server)";
    private static final String R_OUD = "Installation for Oracle Unified Directory";
    private static final String R_OID = "Collocated Oracle Internet Directory Server (Managed through WebLogic server)";
    private static final String R_WCC = "WebCenter Content";
    private static final String R_WCP = "WebCenter Portal";
    private static final String R_WCS = "WebCenter Sites";
    private static final String R_ODI = "Oracle Data Integrator";

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
            case B2B:
                response = R_B2B;
                break;
            case MFT:
                response = R_MFT;
                break;
            case IDM:
                if (FmwInstallerType.IDM == fmwInstallerType) {
                    response = R_IDM;
                } else {
                    response = R_IDM_WLS;
                }
                break;
            case OAM:
                response = R_OAM;
                break;
            case OHS:
                response = R_OHS;
                break;
            case OUD:
                response = R_OUD;
                break;
            case OID:
                response = R_OID;
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
            case ODI:
                response = R_ODI;
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
        try (FileWriter fw = new FileWriter(buildContextDir + File.separator + filename)) {
            mustache.execute(fw, this).flush();
        }
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
