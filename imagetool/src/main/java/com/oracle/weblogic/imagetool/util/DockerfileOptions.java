// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class DockerfileOptions {

    boolean useYum = false;
    boolean useAptGet = false;
    boolean useApk = false;
    boolean useZypper = false;

    private boolean useWdt;
    private boolean applyPatches;
    private boolean updateOpatch;
    private boolean skipJavaInstall;

    private String username;
    private String groupname;
    private String javaHome;
    private String tempDirectory;
    private String baseImageName;
    private ArrayList<String> wdtModelList;

    /**
     * Options to be used with the Mustache template.
     */
    public DockerfileOptions() {
        wdtModelList = new ArrayList<>();

        useWdt = false;
        applyPatches = false;
        updateOpatch = false;
        skipJavaInstall = false;

        username = "oracle";
        groupname = "oracle";

        javaHome = "/u01/jdk";
        tempDirectory = "/tmp/delme";

        baseImageName = "oraclelinux:7-slim";
    }

    /**
     * The userid that should own the JDK and FMW install binaries.
     *
     * @return the userid
     */
    public String userid() {
        return username;
    }

    /**
     * The userid that should own the JDK and FMW install binaries.
     */
    public void setUserId(String value) {
        username = value;
    }

    /**
     * The groupid that should own the JDK and FMW install binaries.
     *
     * @return the groupid
     */
    public String groupid() {
        return groupname;
    }

    /**
     * The groupid that should own the JDK and FMW install binaries.
     */
    public void setGroupId(String value) {
        groupname = value;
    }

    /**
     * The base Docker image that the new image should be based on.
     *
     * @return the image name
     */
    public String baseImage() {
        return baseImageName;
    }

    /**
     * The base Docker image that the new image should be based on.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setBaseImage(String value) {
        baseImageName = value;
        return this;
    }

    /**
     * Only one package installer should be allowed.
     *
     * @param option the String constant identifying the installer to use.
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setPackageInstaller(String option) {
        useZypper = false;
        useAptGet = false;
        useApk = false;
        useYum = false;
        switch (option) {
            case Constants.YUM:
                useYum = true;
                break;
            case Constants.APTGET:
                useAptGet = true;
                break;
            case Constants.ZYPPER:
                useZypper = true;
                break;
            default:
        }
        return this;
    }

    public String java_home() {
        return javaHome;
    }

    /**
     * Set the JAVA_HOME environment variable for the Dockerfile to be written.
     *
     * @param value the folder where JAVA is or should be installed, aka JAVA_HOME.
     */
    public void setJavaHome(String value) {
        javaHome = value;
    }

    /**
     * Disable the Java installation because Java is already installed.
     *
     * @param javaHome the JAVA_HOME from the base image.
     */
    public void disableJavaInstall(String javaHome) {
        this.javaHome = javaHome;
        skipJavaInstall = true;
    }

    /**
     * Referenced by Dockerfile template, for enabling JDK install function.
     *
     * @return true if Java should be installed.
     */
    public boolean installJava() {
        return !skipJavaInstall;
    }

    /**
     * Referenced by Dockerfile template, for enabling patching function.
     *
     * @return true if patching should be performed.
     */
    public boolean isPatchingEnabled() {
        return applyPatches;
    }

    /**
     * Toggle patching ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setPatchingEnabled() {
        applyPatches = true;
        return this;
    }

    /**
     * Referenced by Dockerfile template, for enabling OPatch patching function.
     *
     * @return true if OPatch patching should be performed.
     */
    public boolean isOpatchPatchingEnabled() {
        return updateOpatch;
    }

    /**
     * Toggle OPatch patching ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setOPatchPatchingEnabled() {
        updateOpatch = true;
        return this;
    }

    /**
     * Referenced by Dockerfile template, for enabling WDT function.
     *
     * @return true if WDT domain create should be performed.
     */
    public boolean isWdtEnabled() {
        return useWdt;
    }

    /**
     * If WDT is enabled, and the model is not in the archive, the model file argument must be set.
     * @param value a model filename, or comma-separated model filenames.
     */
    public void setWdtModels(List<String> value) {
        if (value != null) {
            wdtModelList.addAll(value);
        }
    }

    /**
     * Referenced by Dockerfile template, a simple list of model filenames.
     *
     * @return a list of Strings with the model filenames.
     */
    public List<String> wdtModels() {
        return wdtModelList;
    }

    /**
     * Referenced by Dockerfile template, provides the WDT argument for 1..n model files.
     *
     * @return model_file argument for WDT command.
     */
    public String wdtModelFileArgument() {
        StringJoiner result = new StringJoiner(",","-model_file ","");
        result.setEmptyValue("");
        for (String model : wdtModelList) {
            result.add(tempDirectory + "/" + model);
        }
        return result.toString();
    }

    /**
     * Referenced by Dockerfile template, provides temporary location to write/copy files in the Docker image.
     *
     * @return the path to the temporary directory.
     */
    public String tmpDir() {
        return tempDirectory;
    }

    /**
     * Toggle WDT domain creation ON.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setWdtEnabled() {
        useWdt = true;
        return this;
    }

    /**
     * Referenced by Dockerfile template, provides location where installers should write their temporary files.
     *
     * @return the full path to the temporary directory that should be used.
     */
    public String tempDir() {
        return tempDirectory;
    }

    /**
     * The location where installers should write their temporary files.
     *
     * @param value  the full path to the temporary directory that should be used.
     */
    public void setTempDirectory(String value) {

        tempDirectory = value;
    }
}
