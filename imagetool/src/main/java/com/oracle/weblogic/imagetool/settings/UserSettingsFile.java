// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class UserSettingsFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettingsFile.class);
    /**
     * Configured defaults associated with each installer.
     */
    private final EnumMap<InstallerType, InstallerSettings> installerSettings;

    //private InstallerSettings patches = null;

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

    private String installerDetailsFile = null;

    private String patchDetailsFile = null;

    private String defaultBuildPlatform = null;

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
        installerSettings = new EnumMap<>(InstallerType.class);
        settingsFile = new SettingsFile(pathToSettingsFile);
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

    public String getPatchDetailsFile() {
        return patchDetailsFile;
    }

    public void setPatchDetailsFile(String value) {
        patchDetailsFile = value;
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
        return installerSettings.get(installerType);
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
        installerDetailsFile = SettingsFile.getValue("installerSettingsFile", String.class, settings);
        patchDetailsFile = SettingsFile.getValue("patchSettingsFile", String.class, settings);
        // Just the settings about the installer not the individual installers
        installerSettings.clear();
        Map<String, Object> installerFolder = SettingsFile.getFolder("installers", settings);
        for (Map.Entry<String, Object> entry: installerFolder.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isEmpty()) {
                key = key.toUpperCase();
                try {
                    installerSettings.put(
                        InstallerType.valueOf(key),
                        new InstallerSettings((Map<String, Object>) entry.getValue()));
                } catch (IllegalArgumentException illegal) {
                    logger.warning("settings for {0} could not be loaded.  {0} is not a valid installer type: {1}",
                                    key, InstallerType.class.getEnumConstants());
                }
            }
        }

        logger.exiting();
    }

    public String getInstallerDetailsFile() {
        return installerDetailsFile;
    }

    public String setInstallerDetailsFile(String value) {
        return installerDetailsFile = value;
    }

    public String getDefaultBuildPlatform() {
        return defaultBuildPlatform;
    }

    public String setDefaultBuildPlatform(String value) {
        return defaultBuildPlatform = value;
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
            + ", installerDetailsFile='" + installerDetailsFile + '\''
            + ", patchDetailsFile='" + patchDetailsFile + '\''
            + '}';
    }
}
