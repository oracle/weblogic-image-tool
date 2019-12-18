// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.FmwInstallerType;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class MiddlewareInstall {

    private static final LoggingFacade logger = LoggingFactory.getLogger(MiddlewareInstall.class);

    private List<MiddlewareInstallPackage> installerFiles = new ArrayList<>();

    public void copyFiles(CacheStore cacheStore, String buildContextDir) throws IOException {
        for (MiddlewareInstallPackage installPackage: installerFiles) {
            installPackage.installer.copyFile(cacheStore, buildContextDir);
            installPackage.responseFile.copyFile(buildContextDir);
            //TODO: copy response file to context Dir
        }
    }

    public List<MiddlewareInstallPackage> getInstallers() {
        return installerFiles;
    }

    public void setResponseFiles(List<String> responseFiles) {
        if (responseFiles != null && !responseFiles.isEmpty()) {
            if (responseFiles.size() != installerFiles.size()) {
                throw new IllegalArgumentException(
                    "The number of response files did not match the number of installers " + installerFiles.size());
            }
            for (int i = 0; i < installerFiles.size(); i++) {
                installerFiles.get(i).responseFile = new ProvidedResponseFile(responseFiles.get(i));
            }
        }
    }

    /**
     * Get the install metadata for a given middleware install type.
     * @param type the requested middleware install type
     * @return the metadata for the given install type
     */
    public static MiddlewareInstall getInstall(FmwInstallerType type, String version) {
        MiddlewareInstall result = new MiddlewareInstall();

        switch (type) {
            case FMW:
                MiddlewareInstallPackage fmw = new MiddlewareInstallPackage();
                fmw.installer = new CachedFile(InstallerType.FMW, version);
                fmw.responseFile = new DefaultResponseFile("/response-files/fmw.rsp");
                result.installerFiles.add(fmw);
                break;
            case WLS:
                MiddlewareInstallPackage pkg = new MiddlewareInstallPackage();
                pkg.installer = new CachedFile(InstallerType.WLS, version);
                pkg.responseFile = new DefaultResponseFile("/response-files/wls.rsp");
                result.installerFiles.add(pkg);
                break;
            default:
                throw new IllegalArgumentException(type.toString() + " is not a supported middleware install type");
        }
        return result;
    }
}
