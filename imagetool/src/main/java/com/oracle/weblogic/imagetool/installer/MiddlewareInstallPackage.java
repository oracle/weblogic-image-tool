// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.nio.file.Path;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;

public class MiddlewareInstallPackage {
    public InstallerType type;
    public ResponseFile responseFile;
    public CachedFile installer;
    public Path installerPath;
    public String installerFilename;
    public String jarName;
    public List<String> preinstallCommands;
    public boolean isZip = true;
    public boolean isBin = false;
    public String platform;
}
