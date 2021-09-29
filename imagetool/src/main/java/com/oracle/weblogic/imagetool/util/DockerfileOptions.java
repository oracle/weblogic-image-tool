// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.cli.menu.PackageManagerType;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstall;
import com.oracle.weblogic.imagetool.installer.MiddlewareInstallPackage;
import com.oracle.weblogic.imagetool.wdt.WdtOperation;

/**
 * Provides the data used by the Dockerfile templates (in mustache).
 */
public class DockerfileOptions {

    private static final String DEFAULT_JAVA_HOME = "/u01/jdk";
    private static final String DEFAULT_ORACLE_HOME = "/u01/oracle";
    private static final String DEFAULT_DOMAIN_HOME = "/u01/domains/base_domain";
    // Default location for the oraInst.loc
    private static final String DEFAULT_INV_LOC = "/u01/oracle";

    private static final String DEFAULT_ORAINV_DIR = "/u01/oracle/oraInventory";

    final String buildId;

    private boolean useWdt;
    private boolean applyPatches;
    private boolean updateOpatch;
    private boolean skipJavaInstall;
    private boolean skipMiddlewareInstall;
    private boolean isRebaseToTarget;
    private boolean isRebaseToNew;
    private boolean strictPatchOrdering;

    private String javaInstaller;
    private String username;
    private String groupname;
    private String javaHome;
    private String oracleHome;
    private String invLoc;
    private String oraInvDir;
    private String domainHome;
    private String tempDirectory;
    private String baseImageName;
    private String opatchFileName;
    private String sourceImage;
    private String targetImage;
    private PackageManagerType pkgMgr;
    private List<String> patchFilenames;
    private MiddlewareInstall mwInstallers;
    private boolean domainGroupAsUser;

    // WDT values
    private String wdtHome;
    private String wdtModelHome;
    private ArrayList<String> wdtModelList;
    private ArrayList<String> wdtArchiveList;
    private ArrayList<String> wdtVariableList;
    private WdtOperation wdtOperation;
    private String wdtDomainType;
    private String wdtJavaOptions;
    private boolean wdtModelOnly;
    private boolean wdtRunRcu;
    private boolean wdtStrictValidation;
    private String wdtInstallerFilename;
    private String wdtEncryptionKey;
    private String wdtBase;

    // Additional Build Commands
    private Map<String, List<String>> additionalBuildCommands;

    /**
     * Options to be used with the Mustache template.
     */
    public DockerfileOptions(String buildId) {
        this.buildId = buildId;
        applyPatches = false;
        updateOpatch = false;
        skipJavaInstall = false;
        skipMiddlewareInstall = false;
        domainGroupAsUser = false;

        javaHome = DEFAULT_JAVA_HOME;
        oracleHome = DEFAULT_ORACLE_HOME;
        invLoc = DEFAULT_INV_LOC;
        oraInvDir = DEFAULT_ORAINV_DIR;

        tempDirectory = "/tmp/imagetool";

        baseImageName = "ghcr.io/oracle/oraclelinux:7-slim";

        // WDT values
        useWdt = false;
        wdtHome = "/u01/wdt";
        wdtModelHome = wdtHome + "/models";
        wdtOperation = WdtOperation.CREATE;
        wdtModelList = new ArrayList<>();
        wdtArchiveList = new ArrayList<>();
        wdtVariableList = new ArrayList<>();
        wdtRunRcu = false;
        wdtStrictValidation = false;
        wdtBase = "wls_build"; // By default, use output of Oracle Home install
    }

    /**
     * Compare if the child is a subdirectory of the parent (non java.io implementation).
     *
     * @param child  child directory name
     * @param parent parent directory name
     * @return true if child is NOT a subdirectory of parent
     */

    private static boolean isNotSubDirectory(String child, String parent) {
        boolean result = false;
        child = child.endsWith(File.separator) ? child.substring(0, child.length() - 1) : child;
        parent = parent.endsWith(File.separator) ? parent.substring(0, parent.length() - 1) : parent;
        if (!child.equals(parent)) {
            // if the child is not the same as the parent, and the child starts with the same path as parent
            result = !child.startsWith(parent);
        }
        return result;
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
    @SuppressWarnings("unused")
    public String baseImage() {
        return baseImageName;
    }

    /**
     * The base Docker image that the new image should be based on.
     *
     * @return this DockerfileOptions object
     */
    @SuppressWarnings("unused")
    public DockerfileOptions setBaseImage(String value) {
        baseImageName = value;
        return this;
    }

    /**
     * The source Docker image that contain the domain to copy from.
     *
     * @return the image name
     */
    @SuppressWarnings("unused")
    public String sourceImage() {
        return sourceImage;
    }

    /**
     * The source Docker image that contain the domain to copy from.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setSourceImage(String value) {
        sourceImage = value;
        return this;
    }

    /**
     * The target Docker image that contain the domain to copy to.
     *
     * @return the image name
     */
    @SuppressWarnings("unused")
    public String targetImage() {
        return targetImage;
    }

    /**
     * The target Docker image that contain the domain to copy to.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setTargetImage(String value) {
        targetImage = value;
        return this;
    }

    /**
     * The location where the wdt models will be copied to.
     *
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setWdtModelHome(String value) {
        if (!Utils.isEmptyString(value)) {
            wdtModelHome = value;
        }
        return this;
    }

    /**
     * Return if the rebase to existing target image.
     *
     * @return true or false
     */
    @SuppressWarnings("unused")
    public boolean isRebaseToTarget() {
        return isRebaseToTarget;
    }

    /**
     * set the value of rebase to existing target.
     *
     * @param rebaseToTarget true or false
     */
    public void setRebaseToTarget(boolean rebaseToTarget) {
        isRebaseToTarget = rebaseToTarget;
    }

    /**
     * Return if the rebase to existing new image.
     *
     * @return true of false
     */
    @SuppressWarnings("unused")
    public boolean isRebaseToNew() {
        return isRebaseToNew;
    }

    /**
     * set the value of rebase to new target.
     *
     * @param rebaseToNew true or false
     */
    public void setRebaseToNew(boolean rebaseToNew) {
        isRebaseToNew = rebaseToNew;
    }

    /**
     * Set the Linux package Manager type to use during the build.
     *
     * @param option the Linux package Manager type to use during the build.
     * @return this DockerfileOptions object
     */
    public DockerfileOptions setPackageInstaller(PackageManagerType option) {
        pkgMgr = option;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean useYum() {
        return pkgMgr == PackageManagerType.YUM;
    }

    @SuppressWarnings("unused")
    public boolean useMicroDnf() {
        return pkgMgr == PackageManagerType.MICRODNF;
    }

    @SuppressWarnings("unused")
    public boolean useDnf() {
        return pkgMgr == PackageManagerType.DNF;
    }

    @SuppressWarnings("unused")
    public boolean useAptGet() {
        return pkgMgr == PackageManagerType.APTGET;
    }

    @SuppressWarnings("unused")
    public boolean useZypper() {
        return pkgMgr == PackageManagerType.ZYPPER;
    }

    @SuppressWarnings("unused")
    public boolean useApk() {
        return pkgMgr == PackageManagerType.APK;
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
    public String inv_loc() {
        return invLoc;
    }

    @SuppressWarnings("unused")
    public String orainv_dir() {
        return oraInvDir;
    }

    /**
     * Used by Dockerfile template to get the domain home directory.
     * @return the string value of the domain home path
     */
    @SuppressWarnings("unused")
    public String domain_home() {
        if (domainHome != null) {
            return domainHome;
        } else {
            return DEFAULT_DOMAIN_HOME;
        }
    }

    /**
     * Utility function to get the parent directory of the domain home directory.
     * @return the parent path, or the domain home if the domain home does not have a parent.
     */
    @SuppressWarnings("unused")
    public String domain_parent() {
        Path dir = Paths.get(domain_home());
        String parent = dir.getParent().toString();
        return parent != null ? parent : domain_home();
    }

    @SuppressWarnings("unused")
    public String wdt_home() {
        return wdtHome;
    }

    @SuppressWarnings("unused")
    public String wdt_model_home() {
        return wdtModelHome;
    }

    /**
     * Check to see if WDT model home is not under WDT home.
     *
     * @return true|false
     */
    @SuppressWarnings("unused")
    public boolean isWdtModelHomeOutsideWdtHome() {
        return !wdtModelHome.startsWith(wdtHome + File.separator);
    }

    @SuppressWarnings("unused")
    public boolean isWdtValidateEnabled() {
        return isWdtEnabled() && modelOnly() && (!wdtModelList.isEmpty() || !wdtArchiveList.isEmpty());
    }

    /**
     * The directory for the WORKDIR in the Docker build.
     *
     * @return the value for the WORKDIR
     */
    @SuppressWarnings("unused")
    public String work_dir() {
        if ((isWdtEnabled() && !modelOnly()) || sourceImage != null) {
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
     * Set the INV_LOC environment variable for the Dockerfile to be written.
     *
     * @param value the folder where Oracle Middleware is or should be installed, aka INV_LOC.
     */
    public void setInvLoc(String value) {
        if (value != null) {
            invLoc = value;
        } else {
            invLoc = DEFAULT_INV_LOC;
        }
    }

    /**
     * Set the value for the Oracle Inventory directory.
     *
     * @param value the value for inventory file (directory)
     */
    public void setOraInvDir(String value) {
        if (value != null) {
            oraInvDir = value;
        } else {
            oraInvDir = DEFAULT_ORAINV_DIR;
        }
    }

    /**
     * Set the Domain directory to be used by WDT domain creation.
     * WDT -domain_home.
     *
     * @param value the full path to the domain directory
     */
    public DockerfileOptions setDomainHome(String value) {
        domainHome = value;
        return this;
    }

    /**
     * Set the JAVA_HOME environment variable for the Dockerfile to be written.
     *
     * @param value the folder where JAVA is or should be installed, aka JAVA_HOME.
     */
    public DockerfileOptions setJavaHome(String value) {
        if (value != null) {
            javaHome = value;
        } else {
            javaHome = DEFAULT_JAVA_HOME;
        }
        return this;
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
    @SuppressWarnings("unused")
    public boolean installJava() {
        return !skipJavaInstall;
    }

    /**
     * Disable the Middleware installation because an Oracle Home is already installed.
     *
     * @param oracleHome the ORACLE_HOME from the base image.
     */
    public void disableMiddlewareInstall(String oracleHome) {
        this.oracleHome = oracleHome;
        skipMiddlewareInstall = true;
    }

    /**
     * Referenced by Dockerfile template, for enabling Middleware install function.
     *
     * @return true if Java should be installed.
     */
    @SuppressWarnings("unused")
    public boolean installMiddleware() {
        return !skipMiddlewareInstall;
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
    @SuppressWarnings("unused")
    public boolean isWdtEnabled() {
        return useWdt;
    }

    /**
     * Referenced by the Dockerfile template to determine which command to use to install WDT.
     *
     * @return true if the WDT installer file is a tar.gz file; false otherwise.
     */
    public boolean usingWdtTarGzInstaller() {
        return wdtInstallerFilename != null && wdtInstallerFilename.toLowerCase().endsWith(".tar.gz");
    }

    /**
     * copyOraInst check if it needs to copy the oraInst.loc file.
     *
     * @return true if it needs to copy
     */
    @SuppressWarnings("unused")
    public boolean copyOraInst() {
        return isNotSubDirectory(invLoc, oracleHome);
    }

    /**
     * copyOraInventoryDir check if it needs to copy the oraInventory Directory.
     *
     * @return true if it needs to copy
     */
    @SuppressWarnings("unused")
    public boolean copyOraInventoryDir() {
        return isNotSubDirectory(oraInvDir, oracleHome);
    }

    /**
     * If WDT is enabled, and the model is not in the archive, the model file argument must be set.
     *
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
     * If FMW inventory custom location is set.
     *
     * @return true if invLoc is not equal to the default location
     */
    @SuppressWarnings("unused")
    public boolean isCustomInventoryLoc() {
        return !invLoc.equals(DEFAULT_INV_LOC);
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
     *
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
    @SuppressWarnings("unused")
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
        StringJoiner result = new StringJoiner(",", wdtParameterName, "");
        result.setEmptyValue("");
        for (String name : filenames) {
            result.add(wdtModelHome + "/" + name);
        }
        return result.toString();
    }

    /**
     * Set the path to the variable file to be used with WDT.
     *
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
     * Referenced by Dockerfile template, location in the image where the builder should write temporary files.
     *
     * @return the full path to the temporary directory that should be used.
     */
    @SuppressWarnings("unused")
    public String tempDir() {
        return tempDirectory;
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
     * @param value CREATE, DEPLOY, or UPDATE.
     */
    public DockerfileOptions setWdtCommand(WdtOperation value) {
        wdtOperation = value;
        return this;
    }

    /**
     * Enable WDT -use_encryption flag.
     *
     * @param value encryption key to use for WDT decryption
     * @return this (builder)
     */
    public DockerfileOptions setWdtEncryptionKey(String value) {
        wdtEncryptionKey = value;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean isWdtUseEncryption() {
        return wdtEncryptionKey != null;
    }

    /**
     * Referenced by Dockerfile template, provides the encryption key for WDT decryption.
     *
     * @return the encryption key.
     */
    @SuppressWarnings("unused")
    public String wdtEncryptionKey() {
        return wdtEncryptionKey;
    }

    /**
     * Referenced by Dockerfile template, provides the domain type for WDT.
     *
     * @return the name of the WDT domain type.
     */
    @SuppressWarnings("unused")
    public String domainType() {
        return wdtDomainType;
    }

    /**
     * Set the desired WDT domain type parameter.
     *
     * @param value WLS, JRF, ...
     */
    public DockerfileOptions setWdtDomainType(String value) {
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

    @SuppressWarnings("unused")
    public String wdtInstaller() {
        return wdtInstallerFilename;
    }

    public DockerfileOptions setWdtInstallerFilename(String value) {
        wdtInstallerFilename = value;
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
    public DockerfileOptions setAdditionalBuildCommands(Map<String, List<String>> commands) {
        additionalBuildCommands = commands;
        return this;
    }

    private List<String> getAdditionalCommandsForSection(String sectionName) {
        if (additionalBuildCommands == null) {
            return Collections.emptyList();
        }
        return additionalBuildCommands.get(sectionName);
    }

    @SuppressWarnings("unused")
    public boolean strictPatchOrdering() {
        return strictPatchOrdering;
    }

    /**
     * Set strict patch ordering to apply each patch one at a time, instead of
     * in a single pass with OPatch.  This will slow down the patching process but
     * is necessary for some patches.
     *
     * @param value true to enable strict ordering
     * @return this
     */
    public DockerfileOptions setStrictPatchOrdering(boolean value) {
        strictPatchOrdering = value;
        return this;
    }

    /**
     * Set patch file names to be used for OPatch.
     * This list is only used when using strictPatchOrdering.
     *
     * @param patchList list of ARU Patches
     * @return this
     */
    public DockerfileOptions setPatchList(List<AruPatch> patchList) {
        patchFilenames = patchList.stream()
            .map(AruPatch::fileName)
            .collect(Collectors.toList());

        return this;
    }

    @SuppressWarnings("unused")
    public List<String> patches() {
        return patchFilenames;
    }

    /**
     * Referenced by Dockerfile template, provides additional OS packages supplied by the user.
     *
     * @return list of commands as Strings.
     */
    @SuppressWarnings("unused")
    public List<String> osPackages() {
        return getAdditionalCommandsForSection(AdditionalBuildCommands.PACKAGES);
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

    public DockerfileOptions setMiddlewareInstall(MiddlewareInstall install) {
        mwInstallers = install;
        return this;
    }

    @SuppressWarnings("unused")
    public List<MiddlewareInstallPackage> installPackages() {
        return mwInstallers.getInstallers();
    }

    public void setJavaInstaller(String value) {
        javaInstaller = value;
    }

    @SuppressWarnings("unused")
    public String java_pkg() {
        return javaInstaller;
    }

    @SuppressWarnings("unused")
    public String wdtBase() {
        return wdtBase;
    }

    public DockerfileOptions setWdtBase(String value) {
        wdtBase = value;
        return this;
    }

    public DockerfileOptions setDomainGroupAsUser(boolean value) {
        domainGroupAsUser = value;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean domainGroupAsUser() {
        return domainGroupAsUser;
    }
}
