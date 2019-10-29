// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.oracle.weblogic.imagetool.api.model.DomainType;

public class DockerfileOptions {

    private static final String DEFAULT_JAVA_HOME = "/u01/jdk";
    private static final String DEFAULT_ORACLE_HOME = "/u01/oracle";
    private static final String DEFAULT_DOMAIN_HOME = "/u01/domains/base_domain";

    final String buildId;
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
    private String domainHome;
    private String tempDirectory;
    private String baseImageName;
    private String opatchFileName;

    // WDT values
    private String wdtHome;
    private ArrayList<String> wdtModelList;
    private ArrayList<String> wdtArchiveList;
    private ArrayList<String> wdtVariableList;
    private WdtOperation wdtOperation;
    private DomainType wdtDomainType;
    private String wdtJavaOptions;
    private boolean wdtModelOnly;
    private boolean wdtRunRcu;
    private boolean wdtStrictValidation;

    // Additional Build Commands
    private Map<String,List<String>> additionalBuildCommands;

    /**
     * Options to be used with the Mustache template.
     */
    public DockerfileOptions(String buildId) {
        this.buildId = buildId;
        applyPatches = false;
        updateOpatch = false;
        skipJavaInstall = false;

        username = "oracle";
        groupname = "oracle";

        javaHome = DEFAULT_JAVA_HOME;
        oracleHome = DEFAULT_ORACLE_HOME;
        domainHome = DEFAULT_DOMAIN_HOME;
        tempDirectory = "/tmp/imagetool";

        baseImageName = "oraclelinux:7-slim";

        // WDT values
        useWdt = false;
        wdtHome = "/u01/wdt";
        wdtOperation = WdtOperation.CREATE;
        wdtModelList = new ArrayList<>();
        wdtArchiveList = new ArrayList<>();
        wdtVariableList = new ArrayList<>();
        wdtRunRcu = false;
        wdtStrictValidation = false;
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

    @SuppressWarnings("unused")
    public String java_home() {
        return javaHome;
    }

    @SuppressWarnings("unused")
    public String oracle_home() {
        return oracleHome;
    }

    @SuppressWarnings("unused")
    public String domain_home() {
        return domainHome;
    }

    @SuppressWarnings("unused")
    public String wdt_home() {
        return wdtHome;
    }

    @SuppressWarnings("unused")
    public String work_dir() {
        if (isWdtEnabled() && !modelOnly()) {
            return domain_home();
        } else {
            return oracle_home();
        }
    }

    /**
     * Set the ORACLE_HOME environment variable for the Dockerfile to be written.
     *
     * @param value the folder where Oracle Middleware is or should be installed, aka ORACLE_HOME.
     */
    public void setOracleHome(String value) {
        if (value != null) {
            oracleHome = value;
        } else {
            oracleHome = DEFAULT_ORACLE_HOME;
        }
    }

    /**
     * Set the Domain directory to be used by WDT domain creation.
     * WDT -domain_home.
     * @param value the full path to the domain directory
     */
    public void setDomainHome(String value) {
        if (value != null) {
            domainHome = value;
        } else {
            domainHome = DEFAULT_DOMAIN_HOME;
        }
    }

    /**
     * Set the JAVA_HOME environment variable for the Dockerfile to be written.
     *
     * @param value the folder where JAVA is or should be installed, aka JAVA_HOME.
     */
    public void setJavaHome(String value) {
        if (value != null) {
            javaHome = value;
        } else {
            javaHome = DEFAULT_JAVA_HOME;
        }
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
     * Set filename for OPatch patch.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setOPatchFileName(String value) {
        opatchFileName = value;
        return this;
    }

    /**
     * Referenced by Dockerfile template, provides OPatch filename.
     *
     * @return simple filename for OPatch Patch.
     */
    @SuppressWarnings("unused")
    public String opatchFileName() {
        return opatchFileName;
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
    @SuppressWarnings("unused")
    public List<String> wdtModels() {
        return wdtModelList;
    }

    /**
     * Referenced by Dockerfile template, provides the WDT argument for 1..n model files.
     *
     * @return model_file argument for WDT command.
     */
    @SuppressWarnings("unused")
    public String wdtModelFileArgument() {
        return wdtGetFileArgument("-model_file ", wdtModelList);
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
    @SuppressWarnings("unused")
    public List<String> wdtArchives() {
        return wdtArchiveList;
    }

    /**
     * Referenced by Dockerfile template, provides the WDT argument for 1..n archive files.
     *
     * @return archive_file argument for WDT command.
     */
    @SuppressWarnings("unused")
    public String wdtArchiveFileArgument() {
        return wdtGetFileArgument("-archive_file ", wdtArchiveList);
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
    @SuppressWarnings("unused")
    public String wdtVariableFileArgument() {
        return wdtGetFileArgument("-variable_file ", wdtVariableList);
    }

    private String wdtGetFileArgument(String wdtParameterName, List<String> filenames) {
        StringJoiner result = new StringJoiner(",", wdtParameterName,"");
        result.setEmptyValue("");
        for (String name : filenames) {
            result.add(wdtHome + "/models/" + name);
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public boolean runRcu() {
        return wdtRunRcu;
    }

    public DockerfileOptions setRunRcu(boolean value) {
        wdtRunRcu = value;
        return this;
    }


    @SuppressWarnings("unused")
    public boolean strictValidation() {
        return wdtStrictValidation;
    }

    public DockerfileOptions setWdtStrictValidation(boolean value) {
        wdtStrictValidation = value;
        return this;
    }

    @SuppressWarnings("unused")
    public String wlsdeploy_properties() {
        return wdtJavaOptions;
    }

    /**
     * Set the additional commands to be used during the Docker build.
     *
     * @param commands Additional build commands grouped by section.
     */
    public DockerfileOptions setAdditionalBuildCommands(Map<String,List<String>> commands) {
        additionalBuildCommands = commands;
        return this;
    }

    private List<String> getAdditionalCommandsForSection(String sectionName) {
        if (additionalBuildCommands == null) {
            return null;
        }
        return additionalBuildCommands.get(sectionName);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> beforeJdkInstall() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.BEFORE_JDK);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> afterJdkInstall() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.AFTER_JDK);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> beforeFmwInstall() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.BEFORE_FMW);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> afterFmwInstall() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.AFTER_FMW);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> beforeWdtCommand() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.BEFORE_WDT);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> afterWdtCommand() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.AFTER_WDT);
    }

    /**
     * Referenced by Dockerfile template, provides additional build commands supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> finalBuildCommands() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.FINAL_BLD);
    }

}
