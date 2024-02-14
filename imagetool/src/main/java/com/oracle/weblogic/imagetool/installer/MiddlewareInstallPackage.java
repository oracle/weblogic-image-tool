// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import com.oracle.weblogic.imagetool.api.model.CachedFile;

public class MiddlewareInstallPackage {
    private CachedFile installer;
    private String installerFilename;
    private String jarName;
    private boolean isZip = true;
    private boolean isBin = false;
    private String prereqConfigLoc;
    private String prereqFile;
    private ResponseFile responseFile;
    private InstallerType type;


    public String installerFilename() {
        return installerFilename;
    }

    /**
     * The name of the file for this installer.
     *
     * @param value the filename, usually a zip file.
     * @return this, builder
     */
    public MiddlewareInstallPackage installerFilename(String value) {
        installerFilename = value;
        isZip = value.endsWith(".zip");
        return this;
    }

    public boolean isZip() {
        return isZip;
    }

    public boolean isBin() {
        return isBin;
    }

    @SuppressWarnings("unused")
    public String jarName() {
        return jarName;
    }

    /**
     * The name of the JAR file that will be executed as the installer, usually inside the zip file.
     *
     * @param value the name of the JAR, usually inside the zip file of the downloaded installer.
     * @return this, builder
     */
    public MiddlewareInstallPackage jarName(String value) {
        jarName = value;
        isBin = value.endsWith(".bin");
        return this;
    }

    public CachedFile installer() {
        return installer;
    }

    public MiddlewareInstallPackage installer(CachedFile value) {
        installer = value;
        return this;
    }

    @SuppressWarnings("unused")
    public String prereqConfigLoc() {
        return prereqConfigLoc;
    }

    public MiddlewareInstallPackage prereqConfigLoc(String value) {
        prereqConfigLoc = value;
        return this;
    }

    @SuppressWarnings("unused")
    public String prereqFile() {
        return prereqFile;
    }

    /**
     * The name of the file that contains the installer prerequisite "patch".
     *
     * @param value the name of the zip file that contains the prerequisite patch
     * @return this, builder
     */
    public MiddlewareInstallPackage prereqFile(String value) {
        prereqFile = value;
        return this;
    }

    public ResponseFile responseFile() {
        return responseFile;
    }

    public MiddlewareInstallPackage responseFile(ResponseFile value) {
        responseFile = value;
        return this;
    }

    public InstallerType type() {
        return type;
    }

    public MiddlewareInstallPackage type(InstallerType value) {
        type = value;
        return this;
    }
}
