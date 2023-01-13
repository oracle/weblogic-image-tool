// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class UserSettings {
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettings.class);

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    private String buildContextDirectory;

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    private String patchDirectory;

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    private String installerDirectory;

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private String buildEngine;

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private String containerEngine;

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    private Integer aruRetryMax;

    /**
     * The time between each ARU REST call in milliseconds.
     */
    private Integer aruRetryInterval;

    /**
     * The time between each ARU REST call in milliseconds.
     */
    private final Map<String, Object> installers;

    /**
     * Default construct with all default values for settings.
     */
    public UserSettings() {
        patchDirectory = null;
        installerDirectory = null;
        buildContextDirectory = null;
        buildEngine = null;
        containerEngine = null;

        aruRetryMax = null;
        aruRetryInterval = null;
        installers = null;
    }

    /**
     * Extract the Map of settings (i.e., from a YAML file), into this bean, UserSettings.
     * @param settings A map of key-value pairs read in from the YAML user settings file.
     */
    public UserSettings(Map<String, Object> settings) {
        // While yaml.loadAs() will do about the same thing, I opted to use Map because Map is more forgiving.
        // Bad fields or extra data fields not in this version of ImageTool will cause yaml.loadAs to completely fail.
        patchDirectory = getValue("patchDirectory", String.class, settings);
        installerDirectory = getValue("installerDirectory", String.class, settings);
        buildContextDirectory = getValue("buildContextDirectory", String.class, settings);
        buildEngine = getValue("buildEngine", String.class, settings);
        containerEngine = getValue("containerEngine", String.class, settings);

        aruRetryMax = getValue("aruRetryMax", Integer.class, settings);
        aruRetryInterval = getValue("aruRetryInterval", Integer.class, settings);

        installers = getValue("installers", Map.class, settings);
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
     * Loads the settings.yaml file from ~/.imagetool/settings.yaml and returns the values as UserSettings.
     * @return The user settings parsed from ~/.imagetool/settings.yaml
     */
    public static UserSettings load() {
        try (InputStream input = Files.newInputStream(getSettingsFilePath())) {
            return load(input);
        } catch (IOException ioe) {
            logger.fine("Using default setting values, unable to open saved settings: {0}", ioe.getMessage(), ioe);
            return new UserSettings();
        }
    }

    /**
     * Utility method to convert the InputStream with YAML into UserSettings.
     * @param settings An InputStream with the raw YAML text.
     * @return The UserSettings containing the parsed values from the InputStream.
     */
    public static UserSettings load(InputStream settings) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(settings);
        logger.fine("User settings loaded: {0}", map);
        return new UserSettings(map);
    }

    private <T> T getValue(String settingName, Class<T> type, Map<String, Object> settings) {
        if (settings == null) {
            return null;
        }

        Object value = settings.get(settingName);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        logger.severe("Setting for {0} could not be loaded.  Expected {1}, but found {2}. Invalid value: {3}",
            settingName, type, value.getClass(), value.toString());
        return null;
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
        logger.fine("before: {0}", toYamlString());
        patchDirectory = value;
        logger.fine("after: {0}", toYamlString());
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
     * The settings asscociated with the installers to be used.
     * @return a map of settings for installers
     */
    public Map<String,Object> getInstallers() {
        return installers;
    }

    /**
     * Given an installer type, returns the user setting for the default installer version to use.
     * @param installerType Installer type such as JDK, WLS, SOA, etc.
     * @return the user configured default value
     */
    public String getDefaultInstallerVersion(InstallerType installerType) {
        if (installers == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> installerSettings = getValue(installerType.toString(), Map.class, installers);
        return getValue("defaultVersion", String.class, installerSettings);
    }

    private static Representer getYamlRepresenter() {
        // Created this inline override to suppress the output of null for all unset user settings
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
                                                          Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        representer.addClassTag(UserSettings.class, Tag.MAP);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setAllowReadOnlyProperties(true);
        representer.setPropertyUtils(propertyUtils);
        return representer;
    }

    /**
     * Save all settings to the ~/.imagetool/settings.yaml.
     * @throws IOException if an error occurs saving to the filesystem
     */
    public void save() throws IOException {
        save(getSettingsFilePath());
    }

    /**
     * Save all settings to the specified file.
     * @throws IOException if an error occurs saving to the filesystem
     */
    public void save(Path settingsFilePath) throws IOException {
        Path parent = settingsFilePath.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        try (OutputStreamWriter output = new OutputStreamWriter(Files.newOutputStream(settingsFilePath))) {
            Yaml yaml = new Yaml(getYamlRepresenter());
            yaml.dump(this, output);
        } catch (IOException ioe) {
            logger.severe("Failed saved user settings: {0}", ioe.getMessage(), ioe);
            throw ioe;
        }
    }

    /**
     * UserSettings as a YAML string.
     * @return UserSettings as a YAML string.
     */
    public String toYamlString() {
        Yaml yaml = new Yaml(getYamlRepresenter());
        return yaml.dump(this);
    }
}
