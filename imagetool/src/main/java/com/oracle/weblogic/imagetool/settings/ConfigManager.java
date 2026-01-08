// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.util.Architecture;

import static com.oracle.weblogic.imagetool.cachestore.CacheStore.CACHE_DIR_ENV;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.ARCHITECTURE;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.BASE_FMW_VERSION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DATE_ADDED;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DESCRIPTION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DIGEST;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.LOCATION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.PATCH_VERSION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.PRODUCT_VERSION;
import static com.oracle.weblogic.imagetool.util.Utils.getTodayDate;

public class ConfigManager {
    private UserSettingsFile userSettingsFile;
    private SettingsFile installerSettingsFile;
    private SettingsFile patchSettingsFile;
    private static ConfigManager instance;
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettingsFile.class);
    private static CacheStore cacheStore = new CacheStore();

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
        try {
            Path file;
            if (instance == null) {
                String result = System.getenv(CACHE_DIR_ENV);
                if (result != null) {
                    file = Paths.get(result, "settings.yaml");
                } else {
                    file = Paths.get(System.getProperty("user.home"), ".imagetool",
                        "settings.yaml");
                }
                if (!Files.exists(file)) {
                    Files.createDirectories(file.getParent());
                    Files.createFile(file);
                    initImageTool(file);
                }
                return new ConfigManager(file);
            }
            return instance;

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
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

    private static void initImageTool(Path file) throws IOException {
        Path parentPath = file.getParent();
        Files.createFile(Paths.get(parentPath.toString(), "patches.yaml"));
        Files.createFile(Paths.get(parentPath.toString(), "installer.yaml"));
        String dirName = "downloaded_patches";
        Path path = Paths.get(parentPath.toString(), dirName);
        List<String> fileContents = Arrays.asList("patchDirectory: " + path.toString());
        Files.createDirectory(path);
        Files.write(file, fileContents);
    }

    public String getPatchDirectory() {
        return userSettingsFile.getPatchDirectory();
    }

    public String getPatchDetailsFile() {
        return userSettingsFile.returnPatchSettingsFile();
    }

    public String getBuildEngine() {
        return userSettingsFile.getBuildEngine();
    }

    public String getInstallerDetailsFile() {
        return userSettingsFile.returnInstallerSettingsFile();
    }

    /**
     * Return the ARU retry interval.
     * @return retry interval
     */
    public int getAruRetryInterval() {
        return userSettingsFile.getAruRetryInterval();
    }

    /**
     * Return the ARU retry max.
     * @return retry max
     */
    public int getAruRetryMax() {
        return userSettingsFile.getAruRetryMax();
    }

    /**
     * Return the installer setting file.
     * @return SettingsFile
     */
    public SettingsFile getInstallerSettingsFile() {
        if (installerSettingsFile == null) {
            installerSettingsFile = new SettingsFile(Paths.get(userSettingsFile.returnInstallerSettingsFile()));
        }
        return installerSettingsFile;
    }

    /**
     * Return the patch setting file.
     * @return SettingsFile
     */
    public SettingsFile getPatchSettingsFile() {
        if (patchSettingsFile == null) {
            patchSettingsFile = new SettingsFile(Paths.get(userSettingsFile.returnPatchSettingsFile()));
        }
        return patchSettingsFile;
    }

    public String getDefaultBuildPlatform() {
        return userSettingsFile.getDefaultBuildPlatform();
    }

    public Map<String, List<PatchMetaData>> getAllPatches() {
        return cacheStore.getAllPatches();
    }

    public void saveAllPatches(Map<String, List<PatchMetaData>> allPatches) throws IOException {
        cacheStore.saveAllPatches(allPatches);
    }

    public void addPatch(String bugNumber, String patchArchitecture, String patchLocation,
                         String patchVersion, String description) throws IOException {
        cacheStore.addPatch(bugNumber, patchArchitecture, patchLocation, patchVersion, description);
    }

    public void addPatch(String bugNumber, String patchArchitecture, String patchLocation,
                         String patchVersion, String description, String dateAdded) throws IOException {
        cacheStore.addPatch(bugNumber, patchArchitecture, patchLocation, patchVersion, description, dateAdded);
    }

    public PatchMetaData getPatchForPlatform(String platformName,  String bugNumber, String version) {
        return cacheStore.getPatchForPlatform(platformName, bugNumber, version);
    }

    /**
     * Return the metadata for the platformed installer.
     * @param platformName platform name
     * @param installerVersion version of the installer
     * @return InstallerMetaData meta data for the installer
     */
    public InstallerMetaData getInstallerForPlatform(InstallerType installerType, Architecture platformName,
                                                     String installerVersion) {
        return cacheStore.getInstallerForPlatform(installerType, platformName, installerVersion, installerVersion);
    }

    /**
     * Return the metadata for the platformed installer.
     * @param installerType installer type
     * @param platformName platform name
     * @param installerVersion version of the installer
     * @param commonName common name of the installer
     * @return InstallerMetaData meta data for the installer
     */
    public InstallerMetaData getInstallerForPlatform(InstallerType installerType, Architecture platformName,
                                                     String installerVersion, String commonName) {
        return cacheStore.getInstallerForPlatform(installerType, platformName, installerVersion, commonName);
    }

    /**
     * Return the metadata for the platformed installer.
     * @param installerType installer type
     * @param commonName common name of the installer
     * @return InstallerMetaData meta data for the installer
     */
    public List<InstallerMetaData> listInstallerByCommonName(InstallerType installerType, String commonName) {
        return cacheStore.listInstallerByCommonName(installerType, commonName);
    }

    /**
     * Return all the installers based on the configured directory for the yaml file.
     * @return map of installers
     */
    public Map<String, Map<String, List<InstallerMetaData>>> getInstallers() {
        return cacheStore.getInstallers();
    }

    /**
     * Return default wls installer version.
     * @return default wls version if set
     */
    public String getDefaultWLSVersion() {
        return userSettingsFile.returnDefaultWLSVersion();
    }

    /**
     * Return default jdk installer version.
     * @return default wls version if set
     */
    public String getDefaultJDKVersion() {
        return userSettingsFile.returnDefaultJDKVersion();
    }

    /**
     * Return default wdt installer version.
     * @return default wls version if set
     */
    public String getDefaultWDTVersion() {
        return userSettingsFile.returnDefaultWDTVersion();
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
        cacheStore.addInstaller(installerType, commonName, metaData);
    }

    /**
     * Save all installers.
     * @param allInstallers map of installers
     * @throws IOException any error
     */
    public void saveAllInstallers(Map<String, Map<String, List<InstallerMetaData>>> allInstallers) throws IOException {
        cacheStore.saveAllInstallers(allInstallers);
    }

    /**
     * Return the metadata for the patches by bug number.
     * @param bugNumber version of the installer
     * @return list of AruPatch
     */

    public List<AruPatch> getAruPatchForBugNumber(String bugNumber) {
        return cacheStore.getAruPatchForBugNumber(bugNumber);
    }


    private InstallerMetaData createInstallerMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get(DIGEST);
        String dateAdded = (String) objectData.get(DATE_ADDED);
        if (dateAdded == null) {
            dateAdded = getTodayDate();
        }
        String location = (String) objectData.get(LOCATION);
        String productVersion = (String) objectData.get(PRODUCT_VERSION);
        String platform = (String) objectData.get(ARCHITECTURE);
        String baseFMWVersion = (String) objectData.get(BASE_FMW_VERSION);
        return new InstallerMetaData(platform, location, hash, dateAdded, productVersion, baseFMWVersion);
    }

    private PatchMetaData createPatchMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get(DIGEST);
        String dateAdded = (String) objectData.get(DATE_ADDED);
        if (dateAdded == null) {
            dateAdded = getTodayDate();
        }
        String location = (String) objectData.get(LOCATION);
        String productVersion = (String) objectData.get(PATCH_VERSION);
        String platform = (String) objectData.get(ARCHITECTURE);
        String description = (String) objectData.get(DESCRIPTION);
        return new PatchMetaData(platform, location, hash, dateAdded, productVersion, description);
    }

}
