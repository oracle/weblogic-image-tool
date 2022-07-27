// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class UserSettings {
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettings.class);

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    private final String imageBuildDirectory;

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    private final String patchDirectory;

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    private final String installerDirectory;

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private final String buildEngine;

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    private final String containerEngine;

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    private final Integer aruRetryMax;

    /**
     * The time between each ARU REST call in milliseconds.
     */
    private final Integer aruRetryInterval;

    /**
     * Default construct with all default values for settings.
     */
    public UserSettings() {
        patchDirectory = null;
        installerDirectory = null;
        imageBuildDirectory = null;
        buildEngine = null;
        containerEngine = null;

        aruRetryMax = null;
        aruRetryInterval = null;
    }

    /**
     * Extract the Map of settings (from a YAML file), into a Java Bean, UserSettings.
     * @param settings A map of key-value pairs read in from the YAML user settings file.
     */
    public UserSettings(Map<String, Object> settings) {
        // While yaml.loadAs() will do about the same thing, I opted to use Map because Map is more forgiving.
        // Bad fields or extra data fields not in this version of ImageTool will cause yaml.loadAs to completely fail.
        patchDirectory = getValue("patchDirectory", String.class, settings);
        installerDirectory = getValue("installerDirectory", String.class, settings);
        imageBuildDirectory = getValue("imageBuildDirectory", String.class, settings);
        buildEngine = getValue("buildEngine", String.class, settings);
        containerEngine = getValue("containerEngine", String.class, settings);

        aruRetryMax = getValue("aruRetryMax", Integer.class, settings);
        aruRetryInterval = getValue("aruRetryInterval", Integer.class, settings);
    }

    /**
     * The file system path to the directory where the settings file should be.
     * @return The path to ~/.imagetool
     */
    public static Path getSettingsDirectory() {
        return Paths.get(System.getProperty("user.home"), ".imagetool");
    }

    /**
     * Loads the settings.yaml file from ~/.imagetool/settings.yaml and returns the values as UserSettings.
     * @return The user settings parsed from ~/.imagetool/settings.yaml
     */
    public static UserSettings instance() {
        Path settingsFile = getSettingsDirectory().resolve("settings.yaml");
        try (InputStream input = Files.newInputStream(settingsFile)) {
            return load(input);
        } catch (IOException ioe) {
            logger.fine("Unable to open saved settings: {0}", ioe.getMessage(), ioe);
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
        return new UserSettings(map);
    }

    private <T> T getValue(String settingName, Class<T> type, Map<String, Object> settings) {
        Object value = settings.get(settingName);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        } else {
            logger.severe("Setting for {0} could not be loaded.  Expected {1}, but found {2}. Invalid value: {3}",
                settingName, type, value.getClass(), value.toString());
            return null;
        }
    }

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    public String getImageBuildDirectory() {
        if (Utils.isEmptyString(imageBuildDirectory)) {
            return ".";
        }
        return imageBuildDirectory;
    }

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    public String getPatchDirectory() {
        if (Utils.isEmptyString(patchDirectory)) {
            return getSettingsDirectory().resolve("patches").toString();
        }
        return patchDirectory;
    }

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    public String installerDirectory() {
        if (Utils.isEmptyString(installerDirectory)) {
            return getSettingsDirectory().resolve("installers").toString();
        }
        return installerDirectory;
    }

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String getBuildEngine() {
        if (Utils.isEmptyString(buildEngine)) {
            return "docker";
        }
        return buildEngine;
    }

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String getContainerEngine() {
        if (Utils.isEmptyString(containerEngine)) {
            return getBuildEngine();
        }
        return containerEngine;
    }

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    public int getAruRetryMax() {
        if (aruRetryMax == null) {
            return 10;
        }
        return aruRetryMax;
    }

    /**
     * The time between each ARU REST call in milliseconds.
     */
    public int getAruRetryInterval() {
        if (aruRetryInterval == null) {
            return 500;
        }
        return aruRetryInterval;
    }

    /**
     * UserSettings as a YAML string.
     * @return UserSettings as a YAML string.
     */
    public String toYamlString() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setAllowReadOnlyProperties(true);
        Representer representer = new Representer();
        representer.addClassTag(UserSettings.class, Tag.MAP);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        representer.setPropertyUtils(propertyUtils);

        Yaml yaml = new Yaml(representer);
        return yaml.dump(this);
    }
}
