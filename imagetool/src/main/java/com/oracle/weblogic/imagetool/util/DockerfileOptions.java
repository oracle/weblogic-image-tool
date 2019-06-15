/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.weblogic.imagetool.util;

import java.util.List;

public class DockerfileOptions {

    String baseImage = "oraclelinux:7-slim";
    String userid = "oracle";
    String java_home = "/u01/jdk";
    boolean use_WDT = false;
    boolean patching_WLS = false;
    boolean update_opatch = false;

    boolean use_pkg_installer_yum = false;
    boolean use_pkg_installer_apt_get = false;
    boolean use_pkg_installer_apk = false;
    boolean use_pkg_installer_zypper = false;

    private String _groupid = "oracle";

    public String groupid() {
        return _groupid;
    }

    public void setGroupId(String value) {
        _groupid = value;
    }

    public DockerfileOptions(List<String> options) {
        if (options != null && !options.isEmpty()) {
            for (String option : options) {
                switch (option) {
                    case Constants.PATCH:
                        patching_WLS = true;
                        break;
                    case Constants.WDT:
                        use_WDT = true;
                        break;
                    case Constants.OPATCH_PATCH:
                        update_opatch = true;
                        break;
                    case Constants.YUM:
                    case Constants.APTGET:
                    case Constants.ZYPPER:
                        setPackageInstaller(option);
                        break;
                    default:
                }
            }
        }
    }

    /**
     * Only one package installer should be allowed.
     *
     * @param option the String constant identifying the installer to use.
     */
    private void setPackageInstaller(String option) {
        use_pkg_installer_zypper = false;
        use_pkg_installer_apt_get = false;
        use_pkg_installer_apk = false;
        use_pkg_installer_yum = false;
        switch (option) {
            case Constants.YUM:
                use_pkg_installer_yum = true;
                break;
            case Constants.APTGET:
                use_pkg_installer_apt_get = true;
                break;
            case Constants.ZYPPER:
                use_pkg_installer_zypper = true;
                break;
            default:
        }
    }

    /**
     * Set the JAVA_HOME environment variable for the Dockerfile to be written.
     * @param value the folder where JAVA is or should be installed, aka JAVA_HOME.
     */
    public void setJavaHome(String value) {
        java_home = value;
    }
}
