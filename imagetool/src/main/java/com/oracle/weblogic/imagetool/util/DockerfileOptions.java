/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package com.oracle.weblogic.imagetool.util;

import javax.print.Doc;

public class DockerfileOptions {

    boolean use_pkg_installer_yum = false;
    boolean use_pkg_installer_apt_get = false;
    boolean use_pkg_installer_apk = false;
    boolean use_pkg_installer_zypper = false;

    private boolean use_WDT = false;
    private boolean patching_WLS = false;
    private boolean update_opatch = false;
    private String _userid = "oracle";
    private String _groupid = "oracle";
    private String _java_home = "/u01/jdk";
    private String base_image = "oraclelinux:7-slim";

    /**
     * Options to be used with the Mustache template.
     */
    public DockerfileOptions() {
    }

    /**
     * The userid that should own the JDK and FMW install binaries.
     *
     * @return the userid
     */
    public String userid() {
        return _userid;
    }

    /**
     * The userid that should own the JDK and FMW install binaries.
     */
    public void setUserId(String value) {
        _userid = value;
    }

    /**
     * The groupid that should own the JDK and FMW install binaries.
     *
     * @return the groupid
     */
    public String groupid() {
        return _groupid;
    }

    /**
     * The groupid that should own the JDK and FMW install binaries.
     */
    public void setGroupId(String value) {
        _groupid = value;
    }

    /**
     * The base Docker image that the new image should be based on.
     *
     * @return the image name
     */
    public String baseImage() {
        return base_image;
    }

    /**
     * The base Docker image that the new image should be based on.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setBaseImage(String value) {
        base_image = value;
        return this;
    }

    /**
     * Only one package installer should be allowed.
     *
     * @param option the String constant identifying the installer to use.
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setPackageInstaller(String option) {
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
        return this;
    }

    public String java_home() {
        return _java_home;
    }

    /**
     * Set the JAVA_HOME environment variable for the Dockerfile to be written.
     *
     * @param value the folder where JAVA is or should be installed, aka JAVA_HOME.
     */
    public void setJavaHome(String value) {
        _java_home = value;
    }

    /**
     * Referenced by Dockerfile template, for enabling patching function.
     *
     * @return true if patching should be performed.
     */
    public boolean isPatchingEnabled() {
        return patching_WLS;
    }

    /**
     * Toggle patching ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setPatchingEnabled() {
        patching_WLS = true;
        return this;
    }

    /**
     * Referenced by Dockerfile template, for enabling OPatch patching function.
     *
     * @return true if OPatch patching should be performed.
     */
    public boolean isOpatchPatchingEnabled() {
        return update_opatch;
    }

    /**
     * Toggle OPatch patching ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setOPatchPatchingEnabled() {
        update_opatch = true;
        return this;
    }

    /**
     * Referenced by Dockerfile template, for enabling WDT function.
     *
     * @return true if WDT domain create should be performed.
     */
    public boolean isWdtEnabled() {
        return use_WDT;
    }

    /**
     * Toggle WDT domain creation ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setWdtEnabled() {
        use_WDT = true;
        return this;
    }
}
