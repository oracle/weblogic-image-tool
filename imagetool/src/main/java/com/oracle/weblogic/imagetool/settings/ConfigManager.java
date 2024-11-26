// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.util.Architecture;

import static com.oracle.weblogic.imagetool.cachestore.FileCacheStore.CACHE_DIR_ENV;
import static com.oracle.weblogic.imagetool.util.Utils.getTodayDate;

public class ConfigManager {
    private UserSettingsFile userSettingsFile;
    private static ConfigManager instance;
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettingsFile.class);

    private ConfigManager(Path userSettingsFileName) {
        if (userSettingsFileName != null) {
            userSettingsFile = new UserSettingsFile(userSettingsFileName);
        } else {
            userSettingsFile = new UserSettingsFile();
        }
    }

    /**
     * Return the singleton instance of ConfigManager.
     * @return ConfigManager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            String result = System.getenv(CACHE_DIR_ENV);
            if (result != null) {
                if (Files.exists(Paths.get(result, "settings.yaml"))) {
                    return new ConfigManager(Paths.get(result, "settings.yaml"));
                }
            }
            return new ConfigManager(Paths.get(System.getProperty("user.home"), ".imagetool",
                    "settings.yaml"));
        }
        return instance;
    }

    /**
     * Return the singleton instance of ConfigManager.
     * @return ConfigManager instance
     */
    public static synchronized ConfigManager getInstance(Path fileName) {
        // Always reload with file provided
        instance = new ConfigManager(fileName);
        return instance;
    }

    public Map<String, List<PatchMetaData>> getAllPatches() {
        return userSettingsFile.getAllPatches();
    }

    public void saveAllPatches(Map<String, List<PatchMetaData>> allPatches, String location) throws IOException {
        userSettingsFile.saveAllPatches(allPatches, location);
    }

    public void addPatch(String bugNumber, String patchArchitecture, String patchLocation,
                                          String patchVersion, String description) throws IOException {
        userSettingsFile.addPatch(bugNumber, patchArchitecture, patchLocation, patchVersion, description);
    }

    public PatchMetaData getPatchForPlatform(String platformName,  String bugNumber, String version) {
        return userSettingsFile.getPatchForPlatform(platformName, bugNumber, version);
    }

    public String getPatchDirectory() {
        return userSettingsFile.getPatchDirectory();
    }

    public String getPatchDetailsFile() {
        return userSettingsFile.getPatchDetailsFile();
    }

    /**
     * Return the metadata for the platformed installer.
     * @param platformName platform name
     * @param installerVersion version of the installer
     * @return InstallerMetaData meta data for the installer
     */
    public InstallerMetaData getInstallerForPlatform(InstallerType installerType, Architecture platformName,
                                                     String installerVersion) {
        return userSettingsFile.getInstallerForPlatform(installerType, platformName, installerVersion);
    }

    /**
     * Return all the installers based on the configured directory for the yaml file.
     * @return map of installers
     */
    public EnumMap<InstallerType, Map<String, List<InstallerMetaData>>> getInstallers() {

        return userSettingsFile.getInstallers();
    }

    public String getInstallerDetailsFile() {
        return userSettingsFile.getInstallerDetailsFile();
    }

    /**
     * Add installer.
     * @param installerType installer type
     * @param commonName common name
     * @param metaData meta data of the installer
     * @throws IOException when error
     */
    public void addInstaller(InstallerType installerType, String commonName, InstallerMetaData metaData)
        throws IOException {
        userSettingsFile.addInstaller(installerType,commonName, metaData);
    }

    /**
     * Save all installers.
     * @param allInstallers map of installers
     * @param location file location
     * @throws IOException any error
     */
    public void saveAllInstallers(Map<InstallerType, Map<String, List<InstallerMetaData>>> allInstallers,
                                  String location) throws IOException {
        userSettingsFile.saveAllInstallers(allInstallers, location);
    }

    /**
     * Return the metadata for the patches by bug number.
     * @param bugNumber version of the installer
     * @return list of AruPatch
     */

    public List<AruPatch> getAruPatchForBugNumber(String bugNumber) {
        return userSettingsFile.getAruPatchForBugNumber(bugNumber);
    }

    public String getDefaultBuildPlatform() {
        return userSettingsFile.getDefaultBuildPlatform();
    }

    private InstallerMetaData createInstallerMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get("digest");
        String dateAdded = (String) objectData.get("added");
        if (dateAdded == null) {
            dateAdded = getTodayDate();
        }
        String location = (String) objectData.get("location");
        String productVersion = (String) objectData.get("version");
        String platform = (String) objectData.get("platform");
        return new InstallerMetaData(platform, location, hash, dateAdded, productVersion);
    }

    private PatchMetaData createPatchMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get("digest");
        String dateAdded = (String) objectData.get("added");
        if (dateAdded == null) {
            dateAdded = getTodayDate();
        }
        String location = (String) objectData.get("location");
        String productVersion = (String) objectData.get("version");
        String platform = (String) objectData.get("platform");
        String description = (String) objectData.get("description");
        return new PatchMetaData(platform, location, hash, dateAdded, productVersion, description);
    }


}
