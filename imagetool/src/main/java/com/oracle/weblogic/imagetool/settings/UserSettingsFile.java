// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class UserSettingsFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettingsFile.class);
    /**
     * Configured defaults associated with each installer.
     */
    private final Map<String, InstallerSettings> installerSettings;

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    private String buildContextDirectory = null;
    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    private String patchDirectory = null;
    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    private String installerDirectory = null;
    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private String buildEngine = null;
    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private String containerEngine = null;
    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    private Integer aruRetryMax = null;
    /**
     * The time between each ARU REST call in milliseconds.
     */
    private Integer aruRetryInterval = null;

    private final SettingsFile settingsFile;

    private String defaultBuildPlatform = null;

    private String installerSettingsFile = null;
    private String patchSettingsFile = null;

    /**
     * DLoads the settings.yaml file from ~/.imagetool/settings.yaml and applies the values found.
     */
    public UserSettingsFile() {
        this(getSettingsFilePath());
    }

    /**
     * Extract the Map of settings (i.e., from a YAML file), into this bean, UserSettings.
     * Used for internal tests to override default settings file location.
     * @param pathToSettingsFile A map of key-value pairs read in from the YAML user settings file.
     */
    public UserSettingsFile(Path pathToSettingsFile) {
        installerSettings = new HashMap<>();
        settingsFile = new SettingsFile(pathToSettingsFile);
        installerSettingsFile = pathToSettingsFile.getParent().resolve("installers.yaml").toString();
        patchSettingsFile = pathToSettingsFile.getParent().resolve("patches.yaml").toString();
        applySettings(settingsFile.load());
    }

    /**
     * Save all settings to the ~/.imagetool/settings.yaml.
     * @throws IOException if an error occurs saving to the filesystem
     */
    public void save() throws IOException {
        settingsFile.save(this);
    }

    /**
     * The path to the directory where the settings file should be.
     * @return The path to ~/.imagetool
     */
    public static Path getSettingsDirectory() {
        return Paths.get(System.getProperty("user.home"), ".imagetool");
    }

    public static Path getSettingsFilePath() {
        return getSettingsDirectory().resolve("settings.yaml");
    }


    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    public String getBuildContextDirectory() {
        return buildContextDirectory;
    }

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    public void setBuildContextDirectory(String value) {
        buildContextDirectory = value;
    }

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    public String getPatchDirectory() {
        return patchDirectory;
    }

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    public void setPatchDirectory(String value) {
        patchDirectory = value;
    }

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    public String getInstallerDirectory() {
        return installerDirectory;
    }

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    public void setInstallerDirectory(String value) {
        installerDirectory = value;
    }

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String getBuildEngine() {
        return buildEngine;
    }

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public void setBuildEngine(String value) {
        buildEngine = value;
    }

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String getContainerEngine() {
        return containerEngine;
    }

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public void setContainerEngine(String value) {
        containerEngine = value;
    }

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    public Integer getAruRetryMax() {
        return aruRetryMax;
    }

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    public void setAruRetryMax(Integer value) {
        aruRetryMax = value;
    }

    /**
     * The time between each ARU REST call in milliseconds.
     */
    public Integer getAruRetryInterval() {
        return aruRetryInterval;
    }

    /**
     * The time between each ARU REST call in milliseconds.
     */
    public void setAruRetryInterval(Integer value) {
        aruRetryInterval = value;
    }

    /**
     * Set the default WLS version.
     * @param value version of WLS installer
     */
    public void setDefaultWLSVersion(String value) {
        setDefaultVersion(value, InstallerType.WLS);
    }

    private void setDefaultVersion(String value, InstallerType type) {
        Map<String, Object> installerSettingsMap = new HashMap<>();
        installerSettingsMap.put("defaultVersion", value);
        InstallerSettings versionSettings = new InstallerSettings(installerSettingsMap);
        installerSettings.put(type.toString(), versionSettings);
    }

    // Do not use getXXX Snake Yaml will add separate entry in the serialized yaml file
    public String returnDefaultWLSVersion() {
        return Optional.ofNullable(installerSettings.get(InstallerType.WLS.toString()))
            .map(InstallerSettings::getDefaultVersion).orElse(null);
    }

    public void setDefaultWDTVersion(String value) {
        setDefaultVersion(value, InstallerType.WDT);
    }

    public String returnDefaultWDTVersion() {
        return Optional.ofNullable(installerSettings.get(InstallerType.WDT.toString()))
            .map(InstallerSettings::getDefaultVersion).orElse(null);
    }

    public void setDefaultJDKVersion(String value) {
        setDefaultVersion(value, InstallerType.JDK);
    }

    public String returnDefaultJDKVersion() {
        return Optional.ofNullable(installerSettings.get(InstallerType.JDK.toString()))
            .map(InstallerSettings::getDefaultVersion).orElse(null);
    }

    /**
     * The user settings for installer type.
     * @param installerType Installer type such as JDK, WLS, SOA, etc.
     * @return the settings for the requested installer type
     */
    public InstallerSettings getInstallerSettings(InstallerType installerType) {
        if (installerSettings == null) {
            return null;
        }
        return installerSettings.get(installerType.toString());
    }

    public Map<String, InstallerSettings> getInstallerSettings() {
        return installerSettings;
    }

    public String defaultWLSVersion() {
        return "hello";
    }

    private void applySettings(Map<String, Object> settings) {
        logger.entering();
        if (settings == null || settings.isEmpty()) {
            logger.exiting();
            return;
        }

        patchDirectory = SettingsFile.getValue("patchDirectory", String.class, settings);
        installerDirectory = SettingsFile.getValue("installerDirectory", String.class, settings);
        buildContextDirectory = SettingsFile.getValue("buildContextDirectory", String.class, settings);
        buildEngine = SettingsFile.getValue("buildEngine", String.class, settings);
        containerEngine = SettingsFile.getValue("containerEngine", String.class, settings);
        defaultBuildPlatform = SettingsFile.getValue("defaultBuildPlatform", String.class, settings);

        aruRetryMax = SettingsFile.getValue("aruRetryMax", Integer.class, settings);
        aruRetryInterval = SettingsFile.getValue("aruRetryInterval", Integer.class, settings);
        // Just the settings about the installer not the individual installers
        installerSettings.clear();
        Map<String, Object> installerFolder = SettingsFile.getFolder("installers", settings);
        for (Map.Entry<String, Object> entry: installerFolder.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isEmpty()) {
                key = key.toUpperCase();
                try {
                    installerSettings.put(
                        key,
                        new InstallerSettings((Map<String, Object>) entry.getValue()));
                } catch (IllegalArgumentException illegal) {
                    logger.warning("settings for {0} could not be loaded.  {0} is not a valid installer type: {1}",
                                    key, InstallerType.class.getEnumConstants());
                }
            }
        }

        logger.exiting();
    }

    public String getDefaultBuildPlatform() {
        return defaultBuildPlatform;
    }

    public String setDefaultBuildPlatform(String value) {
        return defaultBuildPlatform = value;
    }

    public String returnInstallerSettingsFile() {
        return installerSettingsFile;
    }

    public String returnPatchSettingsFile() {
        return patchSettingsFile;
    }

    @Override
    public String toString() {
        return "UserSettingsFile{"
            + "installerSettings=" + installerSettings
            + ", buildContextDirectory='" + buildContextDirectory + '\''
            + ", patchDirectory='" + patchDirectory + '\''
            + ", installerDirectory='" + installerDirectory + '\''
            + ", buildEngine='" + buildEngine + '\''
            + ", containerEngine='" + containerEngine + '\''
            + ", aruRetryMax=" + aruRetryMax
            + ", aruRetryInterval=" + aruRetryInterval
            + ", settingsFile=" + settingsFile
            + ", installerSettings='" + installerSettings + '\''
            + '}';
    }

}
