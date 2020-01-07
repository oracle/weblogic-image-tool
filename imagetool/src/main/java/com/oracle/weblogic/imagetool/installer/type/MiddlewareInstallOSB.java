// Copyright (c) 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer.type;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.installer.DefaultResponseFile;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstallPackage;

public class MiddlewareInstallOSB extends MiddlewareInstall {

    public MiddlewareInstallOSB(String version) {
        MiddlewareInstallPackage fmw = new MiddlewareInstallPackage();
        fmw.installer = new CachedFile(InstallerType.FMW, version);
        fmw.responseFile = new DefaultResponseFile("/response-files/fmw.rsp");
        addInstaller(fmw);

        MiddlewareInstallPackage osb = new MiddlewareInstallPackage();
        osb.installer = new CachedFile(InstallerType.OSB, version);
        osb.responseFile = new DefaultResponseFile("/response-files/osb.rsp");
        addInstaller(osb);
    }

}
