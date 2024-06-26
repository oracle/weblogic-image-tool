// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;

public class MiddlewareInstallPackage {
    InstallerType type;
    ResponseFile responseFile;
    CachedFile installer;
    String installerFilename;
    String jarName;
    List<String> preinstallCommands;
    boolean isZip = true;
    boolean isBin = false;
}
