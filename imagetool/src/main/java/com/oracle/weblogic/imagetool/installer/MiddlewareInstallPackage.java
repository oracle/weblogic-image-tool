// Copyright (c) 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;

public class MiddlewareInstallPackage {
    public InstallerType type;
    public ResponseFile responseFile;
    public CachedFile installer;
    public String installerFilename;
    public String jarName;
    public boolean isZip = true;
}
