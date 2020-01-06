// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installers;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;

public class WebLogicMiddlewareInstall extends MiddlewareInstall {

    public WebLogicMiddlewareInstall(String version) {
        MiddlewareInstallPackage pkg = new MiddlewareInstallPackage();
        pkg.installer = new CachedFile(InstallerType.WLS, version);
        pkg.responseFile = new DefaultResponseFile("/response-files/wls.rsp");
        addInstaller(pkg);
    }

}
