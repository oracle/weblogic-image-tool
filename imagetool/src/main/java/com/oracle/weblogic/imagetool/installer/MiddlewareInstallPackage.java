// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import com.oracle.weblogic.imagetool.api.model.CachedFile;

public class MiddlewareInstallPackage {
    InstallerType type;
    ResponseFile responseFile;
    CachedFile installer;
    String installerFilename;
    String jarName;
    boolean isZip = true;
    boolean isBin = false;
}
