// Copyright (c) 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer.type;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.installer.DefaultResponseFile;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstallPackage;

public class MiddlewareInstallSOA extends MiddlewareInstall {

    public MiddlewareInstallSOA(String version) {
        MiddlewareInstallPackage fmw = new MiddlewareInstallPackage();
        fmw.installer = new CachedFile(InstallerType.FMW, version);
        fmw.responseFile = new DefaultResponseFile("/response-files/fmw.rsp");
        addInstaller(fmw);

        MiddlewareInstallPackage soa = new MiddlewareInstallPackage();
        soa.installer = new CachedFile(InstallerType.SOA, version);
        soa.responseFile = new DefaultResponseFile("/response-files/soa.rsp");
        addInstaller(soa);
    }

}
