// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.oracle.weblogic.imagetool.api.model.DomainType;

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
    private String oracleHome;
    private String tempDirectory;
    private String baseImageName;

    // WDT values
    private String wdtHome;
    private ArrayList<String> wdtModelList;
    private ArrayList<String> wdtArchiveList;
    private ArrayList<String> wdtVariableList;
    private WdtOperation wdtOperation;
    private DomainType wdtDomainType;
    private String wdtJavaOptions;
    private boolean wdtModelOnly;

    /**
     * Options to be used with the Mustache template.
     */
    public DockerfileOptions() {
        applyPatches = false;
        updateOpatch = false;
        skipJavaInstall = false;

        username = "oracle";
        groupname = "oracle";

        javaHome = "/u01/jdk";
        oracleHome = "/u01/oracle";
        tempDirectory = "/tmp/imagetool";

        baseImageName = "oraclelinux:7-slim";

        // WDT values
        useWdt = false;
        wdtHome = "/u01/wdt";
        wdtOperation = WdtOperation.CREATE;
        wdtModelList = new ArrayList<>();
        wdtArchiveList = new ArrayList<>();
        wdtVariableList = new ArrayList<>();
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

    public String oracle_home() {
        return oracleHome;
    }

    public String wdt_home() {
        return wdtHome;
    }

    public String work_dir() {
        if (isWdtEnabled()) {
            return wdt_home();
        } else {
            return oracle_home();
        }
    }

    public void setOracleHome(String value) {
        oracleHome = value;
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
     * @return this
     */
    public DockerfileOptions setWdtModels(List<String> value) {
        if (value != null) {
            wdtModelList.addAll(value);
        }
        return this;
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
            result.add(wdtHome + "/models/" + model);
        }
        return result.toString();
    }

    /**
     * Set the path to the archive file to be used with WDT.
     * @param value an archive filename, or comma-separated archive filenames.
     * @return this
     */
    public DockerfileOptions setWdtArchives(List<String> value) {
        if (value != null) {
            wdtArchiveList.addAll(value);
        }
        return this;
    }

    /**
     * Referenced by Dockerfile template, a simple list of archive filenames.
     *
     * @return a list of Strings with the archive filenames.
     */
    public List<String> wdtArchives() {
        return wdtArchiveList;
    }

    /**
     * Referenced by Dockerfile template, provides the WDT argument for 1..n archive files.
     *
     * @return archive_file argument for WDT command.
     */
    public String wdtArchiveFileArgument() {
        StringJoiner result = new StringJoiner(",","-archive_file ","");
        result.setEmptyValue("");
        for (String model : wdtArchiveList) {
            result.add(wdtHome + "/archives/" + model);
        }
        return result.toString();
    }

    /**
     * Referenced by Dockerfile template, a simple list of variable filenames.
     *
     * @return a list of Strings with the archive filenames.
     */
    public List<String> wdtVariables() {
        return wdtVariableList;
    }

    /**
     * Referenced by Dockerfile template, provides the WDT argument for 1..n variable files.
     *
     * @return variable_file argument for WDT command.
     */
    public String wdtVariableFileArgument() {
        StringJoiner result = new StringJoiner(",","-variable_file ","");
        result.setEmptyValue("");
        for (String model : wdtVariableList) {
            result.add(wdtHome + "/variables/" + model);
        }
        return result.toString();
    }

    /**
     * Set the path to the variable file to be used with WDT.
     * @param value an variable filename, or comma-separated variable filenames.
     * @return this
     */
    public DockerfileOptions setWdtVariables(List<String> value) {
        if (value != null) {
            wdtVariableList.addAll(value);
        }
        return this;
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

    /**
     * Referenced by Dockerfile template, provides the command to run for WDT.  The default is createDomain.sh.
     *
     * @return the name of the WDT script file.
     */
    public String wdtCommand() {
        return wdtOperation.getScript();
    }

    /**
     * Set the desired WDT Operation to use during update.
     *
     * @param value  CREATE, DEPLOY, or UPDATE.
     */
    public DockerfileOptions setWdtCommand(WdtOperation value) {
        wdtOperation = value;
        return this;
    }

    /**
     * Referenced by Dockerfile template, provides the domain type for WDT.
     *
     * @return the name of the WDT domain type.
     */
    public String domainType() {
        return wdtDomainType.name();
    }

    /**
     * Set the desired WDT domain type parameter.
     *
     * @param value  WLS, JRF, ...
     */
    public DockerfileOptions setWdtDomainType(DomainType value) {
        wdtDomainType = value;
        return this;
    }

    public DockerfileOptions setJavaOptions(String value) {
        wdtJavaOptions = value;
        return this;
    }

    public DockerfileOptions setWdtModelOnly(boolean value) {
        wdtModelOnly = value;
        return this;
    }

    public boolean modelOnly() {
        return wdtModelOnly;
    }

    public String wlsdeploy_properties() {
        return wdtJavaOptions;
    }
}
