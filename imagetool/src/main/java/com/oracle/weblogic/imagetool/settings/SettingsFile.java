// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class SettingsFile {
    private static final LoggingFacade logger = LoggingFactory.getLogger(SettingsFile.class);

    private Path filePath;

    public SettingsFile(Path pathToFile) {
        filePath = pathToFile;
    }

    /**
     * Utility method to convert the InputStream with YAML into UserSettings.
     * @param settings An InputStream with raw YAML text.
     * @return The UserSettings containing the parsed values from the InputStream.
     */
    private Map<String, Object> load(InputStream settings) {
        // While yaml.loadAs() will do about the same thing, I opted to use Map because Map is more forgiving.
        // Bad fields or extra data fields not in this version of ImageTool will cause yaml.loadAs to completely fail.
        logger.entering();
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(settings);
        logger.exiting(map);
        return map;
    }

    /**
     * Loads the settings from the YAML file and returns the loaded file as a Map.
     */
    public Map<String, Object> load() {
        logger.entering(filePath);
        Map<String, Object> map = Collections.emptyMap();
        try (InputStream input = Files.newInputStream(filePath)) {
            map = load(input);
        } catch (IOException ioe) {
            // If exception occurs, map will be empty.  parseMap() must handle both empty and populated map.
        }
        logger.exiting(map);
        return map;
    }

    static class CustomRepresenter extends Representer {
        private DumperOptions options;

        public CustomRepresenter(DumperOptions options) {
            super(options);
            this.options = options;
            this.representers.put(EnumMap.class, data -> representMapping(Tag.MAP, (Map<?,?>)data,
                null));
        }

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

    }

    private static Representer getYamlRepresenter() {
        // Created this inline override to suppress the output of null for all unset user settings
        //DumperOptions options = new DumperOptions();
        //options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        //CustomRepresenter representer = new CustomRepresenter(options);
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
                                                          Tag customTag) {
                // if value of property is null, ignore it.
                this.representers.put(EnumMap.class, data -> representMapping(Tag.MAP, (Map<?,?>)data, null));
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        representer.addClassTag(UserSettingsFile.class, Tag.MAP);
        representer.addClassTag(InstallerType.class, Tag.MAP);
        representer.addClassTag(InstallerSettings.class, Tag.MAP);
        representer.addClassTag(InstallerMetaData.class, Tag.MAP);
        representer.addClassTag(PatchMetaData.class, Tag.MAP);

        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setAllowReadOnlyProperties(true);
        representer.setPropertyUtils(propertyUtils);
        return representer;
    }

    /**
     * Save all settings to the specified file.
     * @throws IOException if an error occurs saving to the filesystem
     */
    public void save(Object data) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        try (OutputStreamWriter output = new OutputStreamWriter(Files.newOutputStream(filePath))) {
            //DumperOptions options = new DumperOptions();
            //options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(getYamlRepresenter());
            yaml.dump(data, output);
        } catch (IOException ioe) {
            logger.severe("Failed saved user settings: {0}", ioe.getMessage(), ioe);
            throw ioe;
        }
    }

    /**
     * Get the value for the setting by name from the provided settings map.
     * This method is intended to be used for leaf attributes (not folders).
     * @param settingName The attribute name of the setting
     * @param type The type of the attribute value (for cast)
     * @param settings The map of settings from which the attribute is to be retrieved
     * @return The value of the requested attribute.
     */
    public static <T> T getValue(String settingName, Class<T> type, Map<String, Object> settings) {
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
     * Get the value for the setting by name from the provided settings map or default value.
     * This method is intended to be used for leaf attributes (not folders).
     * @param settingName The attribute name of the setting
     * @param type The type of the attribute value (for cast)
     * @param settings The map of settings from which the attribute is to be retrieved
     * @param <T> Default value
     * @return The value of the requested attribute.
     */
    public static <T> T getValueOrDefault(String settingName, Class<T> type, Map<String, Object> settings,
                                  Object defaultValue) {
        Object result = SettingsFile.getValue(settingName, type, settings);
        if (result == null && defaultValue != null) {
            result = defaultValue;
        }
        return type.cast(result);
    }

    /**
     * Get the folder for the settings by name from the provided settings map.
     * For nested maps withing the settings map.
     *
     * @param folderName The key/name of the map/folder to be returned
     * @param settings The settings from which the map/folder is to be retrieved.
     * @return A map of settings matching the folder name provided, or empty map if not found.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getFolder(String folderName, Map<String, Object> settings) {
        if (settings == null) {
            return Collections.emptyMap();
        }

        Object value = settings.get(folderName);
        if (value == null) {
            return Collections.emptyMap();
        }

        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }

        //TODO needs to be an exception that will be caught and displayed as a parsing config error.
        logger.severe("Setting for {0} could not be loaded.  Invalid value: {3}",
            folderName, value.toString());
        return Collections.emptyMap();
    }

    /**
     * Convert settings object to a YAML string.
     * @return formatted YAML text.
     */
    public static String asYaml(Object value) {
        Yaml yaml = new Yaml(getYamlRepresenter());
        return yaml.dump(value);
    }
}
