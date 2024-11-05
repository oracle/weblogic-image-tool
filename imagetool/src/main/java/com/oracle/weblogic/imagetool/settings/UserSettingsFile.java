// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.util.Architecture;

import static com.oracle.weblogic.imagetool.aru.AruUtil.getAruPlatformId;
import static com.oracle.weblogic.imagetool.util.Utils.getTodayDate;

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

    public String getPatchDetailsFile() {
        return patchDetailsFile;
    }

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

    public Map<String, InstallerMetaData> getInstaller(InstallerType installerType) {
        return null;
    }

    private InstallerMetaData createInstallerMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get("digest");
        String dateAdded = getTodayDate();
        String location = (String) objectData.get("location");
        String productVersion = (String) objectData.get("version");
        String platform = (String) objectData.get("platform");
        return new InstallerMetaData(platform, location, hash, dateAdded, productVersion);
    }

    private PatchMetaData createPatchMetaData(Map<String, Object> objectData) {
        String hash = (String) objectData.get("digest");
        String dateAdded = getTodayDate();
        String location = (String) objectData.get("location");
        String productVersion = (String) objectData.get("version");
        String platform = (String) objectData.get("platform");
        return new PatchMetaData(platform, location, hash, dateAdded, productVersion);
    }

    /**
     * Return all the installers based on the configured directory for the yaml file.
     * @return map of installers
     */
    public EnumMap<InstallerType, Map<String, List<InstallerMetaData>>> getInstallers() {


        // installers is a list of different installer types jdk, fmw, wdt etc ..
        // For each installer type,  there is a list of individual installer
        //jdk:
        //   11u22:
        //     - platform: linux/arm64
        //       file: /path/to/installerarm.gz
        //       digest: e6a8e178e73aea2fc512799423822bf065758f5e
        //       version: 11.0.22
        //       added: 20241201
        //    - platform: linux/amd64
        //      file: /path/to/installeramd.gz
        //      digest: 1d6dc346ba26bcf1d0c6b5efb030e0dd2f842add
        //      version: 11.0.22
        //      added: 20241201
        //   8u401:
        //wls:
        //  12.2.1.4.0:
        //    - platform: linux/arm64
        //        ....
        //    - platform: linux/arm64

        Map<String, Object> allInstallers = new SettingsFile(Paths.get(installerDetailsFile)).load();
        if (allInstallers == null) {
            allInstallers = new HashMap<>();
        }
        EnumMap<InstallerType, Map<String, List<InstallerMetaData>>> installerDetails
            = new EnumMap<>(InstallerType.class);
        for (Map.Entry<String, Object> entry: allInstallers.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isEmpty()) {
                Map<String, List<InstallerMetaData>> installerMetaData = new HashMap<>();
                key = key.toUpperCase();  // jdk, wls, fmw etc ...
                try {
                    // process list of individual installers
                    // 12.2.1.4.0:
                    //   - platform: linux/arm64
                    //   - platform: linux/amd64
                    // 14.1.2.0.0:
                    //   - platform:
                    //   - platform
                    Map<String, Object> installerValues = (Map<String, Object>) entry.getValue();

                    for (Map.Entry<String, Object> individualInstaller: installerValues.entrySet()) {
                        String individualInstallerKey = individualInstaller.getKey();  // e.g. 12.2.1.4, 14.1.2
                        List<InstallerMetaData> installerMetaDataList = new ArrayList<>(installerValues.size());

                        if (individualInstaller.getValue() instanceof ArrayList) {
                            for (Object installerValue: (ArrayList<Object>) individualInstaller.getValue()) {
                                installerMetaDataList.add(createInstallerMetaData((Map<String, Object>)installerValue));
                            }
                        } else {
                            installerMetaDataList.add(
                                createInstallerMetaData((Map<String, Object>)individualInstaller.getValue()));
                        }
                        installerMetaData.put(individualInstallerKey, installerMetaDataList);
                    }

                    installerDetails.put(InstallerType.valueOf(key), installerMetaData);

                } catch (IllegalArgumentException illegal) {
                    logger.warning("{0} could not be loaded: {1}",
                        key, InstallerType.class.getEnumConstants());
                }
            }
        }
        return installerDetails;
    }


    /**
     * Add installer to local file.
     * @param installerType installer type
     * @param commonName common name used
     * @param metaData installer metadata
     */
    public void addInstaller(InstallerType installerType, String commonName, InstallerMetaData metaData)
        throws IOException {

        EnumMap<InstallerType, Map<String, List<InstallerMetaData>>> installerDetails = getInstallers();
        Map<String, List<InstallerMetaData>> installerMetaDataMap;
        List<InstallerMetaData> installerMetaDataList;

        if (installerDetails.containsKey(installerType)) {
            installerMetaDataMap = installerDetails.get(installerType);
        } else {
            installerDetails.put(installerType, new HashMap<>());
            installerMetaDataMap = installerDetails.get(installerType);
        }
        if (installerMetaDataMap.containsKey(commonName)) {
            installerMetaDataList = installerMetaDataMap.get(commonName);
        } else {
            installerMetaDataMap.put(commonName, new ArrayList<>());
            installerMetaDataList = installerMetaDataMap.get(commonName);
        }
        installerMetaDataList.add(metaData);
        // Update the list
        installerDetails.put(installerType, installerMetaDataMap);
        saveAllInstallers(installerDetails, installerDetailsFile);
    }

    /**
     * Return the metadata for the platformed installer.
     * @param platformName platform name
     * @param installerVersion version of the installer
     * @return InstallerMetaData meta data for the installer
     */

    public InstallerMetaData getInstallerForPlatform(InstallerType installerType, Architecture platformName,
                                                     String installerVersion) {

        if (platformName == null) {
            platformName = Architecture.GENERIC;
        }
        Map<String, List<InstallerMetaData>> installers = getInstallers().get(installerType);
        if (installers != null && !installers.isEmpty()) {
            List<InstallerMetaData> installerMetaDataList = installers.get(installerVersion);
            if (installerMetaDataList != null && !installerMetaDataList.isEmpty()) {
                for (InstallerMetaData installerMetaData: installerMetaDataList) {
                    if (platformName.getAcceptableNames().contains(installerMetaData.getPlatform())) {
                        return installerMetaData;
                    }
                }
            }
        }
        return null;

    }

    /**
     * Add a patch to the local system.
     * @param bugNumber bug number
     * @param patchArchitecture architecture of the patch
     * @param patchLocation file location of the patch
     * @param patchVersion version of the patch
     * @throws IOException error
     */
    public void addPatch(String bugNumber, String patchArchitecture, String patchLocation,
                           String patchVersion) throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> patches = configManager.getAllPatches();
        List<PatchMetaData> latestPatches = patches.get(bugNumber);
        PatchMetaData latestPatch = new PatchMetaData(patchArchitecture, patchLocation, patchVersion);
        latestPatches.add(latestPatch);
        patches.put(bugNumber, latestPatches);
        configManager.saveAllPatches(patches, configManager.getPatchDetailsFile());
    }

    /**
     * Return the metadata for the platformed installer.
     * @param platformName platform name
     * @param bugNumber version of the installer
     * @return InstallerMetaData meta data for the installer
     */

    public PatchMetaData getPatchForPlatform(String platformName,  String bugNumber, String version) {
        Map<String, List<PatchMetaData>> patches = getAllPatches();
        if (patches != null && !patches.isEmpty()) {
            List<PatchMetaData> patchMetaDataList = patches.get(bugNumber);
            if (patchMetaDataList != null && !patchMetaDataList.isEmpty()) {
                for (PatchMetaData patchMetaData: patchMetaDataList) {
                    if (platformName == null || platformName.isEmpty()) {
                        if (patchMetaData.getPlatform().equalsIgnoreCase("Generic")
                            && patchMetaData.getPatchVersion().equals(version)) {
                            return patchMetaData;
                        }
                    } else {
                        if (patchMetaData.getPlatform().equalsIgnoreCase(platformName)
                            && patchMetaData.getPatchVersion().equals(version)) {
                            return patchMetaData;
                        }
                    }
                }
                // search for generic for opatch only??
                if (OPatchFile.DEFAULT_BUG_NUM.equals(bugNumber)) {
                    for (PatchMetaData patchMetaData: patchMetaDataList) {
                        if ("generic".equals(patchMetaData.getPlatform())) {
                            return patchMetaData;
                        }
                    }
                }
            }

        }
        return null;
    }

    /**
     * Return the list of AruPatch data from  for the patches by bug number from local.
     * @param bugNumber version of the installer
     * @return list of AruPatch
     */

    public List<AruPatch> getAruPatchForBugNumber(String bugNumber) {
        Map<String, List<PatchMetaData>> patches = getAllPatches();
        List<AruPatch> aruPatchList = new ArrayList<>();
        if (patches != null && !patches.isEmpty()) {
            List<PatchMetaData> resultPatchMetaDataList = patches.get(bugNumber);
            if (resultPatchMetaDataList != null && !resultPatchMetaDataList.isEmpty()) {
                for (PatchMetaData patchMetaData: resultPatchMetaDataList) {
                    AruPatch aruPatch = new AruPatch();

                    aruPatch.platformName(patchMetaData.getPlatform())
                        .platform(getAruPlatformId(patchMetaData.getPlatform()))
                        .patchId(bugNumber)
                        .fileName(patchMetaData.getLocation())
                        .version(patchMetaData.getPatchVersion());
                    aruPatchList.add(aruPatch);
                }
            }
        }

        return aruPatchList;
    }

    /**
     * return all the patches.
     * @return patch settings
     */
    public Map<String, List<PatchMetaData>> getAllPatches() {


        Map<String, Object> allPatches = new SettingsFile(Paths.get(patchDetailsFile)).load();
        Map<String, List<PatchMetaData>> patchList = new HashMap<>();
        if (allPatches != null && !allPatches.isEmpty()) {
            for (Map.Entry<String, Object> entry: allPatches.entrySet()) {
                String key = entry.getKey(); // bug number
                List<PatchMetaData> patchMetaDataList = new ArrayList<>();
                if (key != null) {
                    if (entry.getValue() instanceof ArrayList) {
                        for (Object installerValue: (ArrayList<Object>) entry.getValue()) {
                            patchMetaDataList.add(createPatchMetaData((Map<String, Object>)installerValue));
                        }
                    } else {
                        patchMetaDataList.add(createPatchMetaData((Map<String, Object>)entry.getValue()));
                    }
                }
                patchList.put(key, patchMetaDataList);
            }
        }
        return patchList;
    }

    /**
     * Save all patches in the local metadata file.
     * @param allPatches Map of all patch metadata
     * @param location file location for store
     * @throws IOException when error
     */
    public void saveAllPatches(Map<String, List<PatchMetaData>> allPatches, String location) throws IOException {
        Map<String, Object> patchList = new HashMap<>();
        for (Map.Entry<String, List<PatchMetaData>> entry: allPatches.entrySet()) {
            String key = entry.getKey(); // bug number
            if (key != null && !key.isEmpty()) {
                ArrayList<Object> list = new ArrayList<>();
                if (entry.getValue() instanceof ArrayList) {
                    for (PatchMetaData patchMetaData: entry.getValue()) {
                        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                        map.put("version", patchMetaData.getPatchVersion());
                        map.put("location", patchMetaData.getLocation());
                        map.put("digest", patchMetaData.getHash());
                        map.put("added", patchMetaData.getDateAdded());
                        map.put("platform", patchMetaData.getPlatform());
                        list.add(map);
                    }
                } else {
                    PatchMetaData patchMetaData = (PatchMetaData) entry.getValue();
                    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                    map.put("version", patchMetaData.getPatchVersion());
                    map.put("location", patchMetaData.getLocation());
                    map.put("digest", patchMetaData.getHash());
                    map.put("added", patchMetaData.getDateAdded());
                    map.put("platform", patchMetaData.getPlatform());
                    list.add(map);
                }
                patchList.put(key, list);
            }
        }
        new SettingsFile(Paths.get(location)).save(patchList);
    }

    /**
     * Save all installers in the local metadata file.
     * @param allInstallers Map of all installers metadata
     * @param location file location for store
     * @throws IOException when error
     */
    public void saveAllInstallers(Map<InstallerType, Map<String, List<InstallerMetaData>>> allInstallers,
                                     String location) throws IOException {
        LinkedHashMap<String, Object> installerList = new LinkedHashMap<>();

        if (allInstallers != null && !allInstallers.isEmpty()) {
            for (Map.Entry<InstallerType, Map<String, List<InstallerMetaData>>> entry: allInstallers.entrySet()) {
                InstallerType installerType = entry.getKey();
                Map<String, List<InstallerMetaData>> installerMetaDataList = entry.getValue();
                LinkedHashMap<String, Object> typedInstallers = new LinkedHashMap<>();

                for (String installerMetaData : installerMetaDataList.keySet()) {
                    List<InstallerMetaData> mdList = installerMetaDataList.get(installerMetaData);
                    ArrayList<Object> installerMetaDataArray = new ArrayList<>();
                    for (InstallerMetaData md : mdList) {
                        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                        map.put("version", md.getProductVersion());
                        map.put("location", md.getLocation());
                        map.put("digest", md.getDigest());
                        map.put("added", md.getDateAdded());
                        map.put("platform", md.getPlatform());
                        installerMetaDataArray.add(map);
                    }
                    typedInstallers.put(installerMetaData, installerMetaDataArray);
                }
                installerList.put(installerType.toString(), typedInstallers);
            }
        }
        new SettingsFile(Paths.get(location)).save(installerList);
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

    public String getDefaultBuildPlatform() {
        return defaultBuildPlatform;
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
