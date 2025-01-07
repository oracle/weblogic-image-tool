// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.settings.SettingsFile;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;

import static com.oracle.weblogic.imagetool.aru.AruUtil.getAruPlatformId;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.ARCHITECTURE;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DATE_ADDED;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DESCRIPTION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.DIGEST;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.LOCATION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.PATCH_VERSION;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.PRODUCT_VERSION;
import static com.oracle.weblogic.imagetool.util.Utils.getTodayDate;

/**
 * This is the helper class that helps keep track of application metadata like
 * which patches have been downloaded and their location on disk.
 */
public class CacheStore {

    public static final String CACHE_KEY_SEPARATOR = "_";
    public static final String CACHE_DIR_ENV = "WLSIMG_CACHEDIR";
    private static final LoggingFacade logger = LoggingFactory.getLogger(UserSettingsFile.class);


    /**
     * Return all the installers based on the configured directory for the yaml file.
     * @return map of installers
     */
    public Map<String, Map<String, List<InstallerMetaData>>> getInstallers() {


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

        //ConfigManager cm = ConfigManager.getInstance();
        //Map<String, Object> allInstallers = cm.getInstallerSettingsFile().load();

        Map<String, Object> allInstallers = new SettingsFile(Paths.get(ConfigManager.getInstance()
            .getInstallerDetailsFile())).load();

        if (allInstallers == null) {
            allInstallers = new HashMap<>();
        }
        Map<String, Map<String, List<InstallerMetaData>>> installerDetails
            = new HashMap<>();
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

                    installerDetails.put(key, installerMetaData);

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

        Map<String, Map<String, List<InstallerMetaData>>> installerDetails = getInstallers();
        Map<String, List<InstallerMetaData>> installerMetaDataMap;
        List<InstallerMetaData> installerMetaDataList;
        String installerKey = installerType.toString().toUpperCase();

        if (installerDetails.containsKey(installerKey)) {
            installerMetaDataMap = installerDetails.get(installerKey);
        } else {
            installerDetails.put(installerKey, new HashMap<>());
            installerMetaDataMap = installerDetails.get(installerKey);
        }

        if (installerMetaDataMap.containsKey(commonName)) {
            installerMetaDataList = installerMetaDataMap.get(commonName);
        } else {
            installerMetaDataMap.put(commonName, new ArrayList<>());
            installerMetaDataList = installerMetaDataMap.get(commonName);
        }
        // Before adding see if one same already existed.
        if (installerMetaDataList.contains(metaData)) {
            logger.info("IMG-0135", metaData.toString());
        } else {
            installerMetaDataList.add(metaData);
        }
        // Update the list
        installerDetails.put(installerKey, installerMetaDataMap);
        saveAllInstallers(installerDetails);
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
        if (installerType == null) {
            installerType = InstallerType.WLS;
        }
        String installerKey = installerType.toString().toUpperCase();

        if (installerVersion == null) {
            switch (installerType) {
                case WLS:
                    installerVersion = ConfigManager.getInstance().getDefaultWLSVersion();
                    break;
                case JDK:
                    installerVersion = ConfigManager.getInstance().getDefaultJDKVersion();
                    break;
                case WDT:
                    installerVersion = ConfigManager.getInstance().getDefaultWDTVersion();
                    break;
                default:
                    break;
            }
            if (installerVersion == null) {
                logger.throwing(new IllegalArgumentException("Cannot determine installer version for installer type "
                    + installerType.toString()));
            }
        }
        Map<String, List<InstallerMetaData>> installers = getInstallers().get(installerKey);
        if (installers != null && !installers.isEmpty()) {
            List<InstallerMetaData> installerMetaDataList = installers.get(installerVersion);
            if (installerMetaDataList != null && !installerMetaDataList.isEmpty()) {
                for (InstallerMetaData installerMetaData: installerMetaDataList) {
                    if (platformName.getAcceptableNames().contains(installerMetaData.getArchitecture())) {
                        return installerMetaData;
                    }
                }
                if (Utils.isGenericInstallerAcceptable(installerType)) {
                    //If it can't find the specialized platform, try generic.
                    for (InstallerMetaData installerMetaData: installerMetaDataList) {
                        if (Architecture.GENERIC.getAcceptableNames().contains(installerMetaData.getArchitecture())) {
                            return installerMetaData;
                        }
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
                         String patchVersion, String description) throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> patches = configManager.getAllPatches();
        List<PatchMetaData> latestPatches = patches.get(bugNumber);
        if (latestPatches == null) {
            latestPatches = new ArrayList<>();
        }
        PatchMetaData newPatch = new PatchMetaData(patchArchitecture, patchLocation, patchVersion, description);
        if (latestPatches.contains(newPatch)) {
            logger.info("IMG-0136", newPatch);
        } else {
            latestPatches.add(newPatch);
        }
        patches.put(bugNumber, latestPatches);
        configManager.saveAllPatches(patches);
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
                         String patchVersion, String description, String dateAdded) throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        Map<String, List<PatchMetaData>> patches = configManager.getAllPatches();
        List<PatchMetaData> latestPatches = patches.get(bugNumber);
        if (latestPatches == null) {
            latestPatches = new ArrayList<>();
        }
        PatchMetaData newPatch = new PatchMetaData(patchArchitecture, patchLocation, patchVersion, description,
            dateAdded);
        if (latestPatches.contains(newPatch)) {
            logger.info("IMG-0136", newPatch.toString());
        } else {
            latestPatches.add(newPatch);
        }
        patches.put(bugNumber, latestPatches);
        configManager.saveAllPatches(patches);
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
                        if (patchMetaData.getArchitecture().equalsIgnoreCase("Generic")
                            && patchMetaData.getPatchVersion().equals(version)) {
                            return patchMetaData;
                        }
                    } else {
                        if (patchMetaData.getArchitecture().equalsIgnoreCase(platformName)
                            && patchMetaData.getPatchVersion().equals(version)) {
                            return patchMetaData;
                        }
                    }
                }
                // search for generic for opatch only??
                if (OPatchFile.DEFAULT_BUG_NUM.equals(bugNumber)) {
                    for (PatchMetaData patchMetaData: patchMetaDataList) {
                        if ("generic".equalsIgnoreCase(patchMetaData.getArchitecture())) {
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

                    aruPatch.platformName(patchMetaData.getArchitecture())
                        .platform(getAruPlatformId(patchMetaData.getArchitecture()))
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


        Map<String, Object> allPatches = new SettingsFile(Paths.get(ConfigManager.getInstance()
            .getPatchDetailsFile())).load();
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
     * @throws IOException when error
     */
    public void saveAllPatches(Map<String, List<PatchMetaData>> allPatches) throws IOException {

        ConfigManager configManager = ConfigManager.getInstance();
        configManager.getPatchSettingsFile().save(allPatches);

    }

    /**
     * Save all installers in the local metadata file.
     * @param allInstallers Map of all installers metadata
     * @throws IOException when error
     */
    public void saveAllInstallers(Map<String, Map<String, List<InstallerMetaData>>> allInstallers) throws IOException {

        ConfigManager.getInstance().getInstallerSettingsFile().save(allInstallers);
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
        return new InstallerMetaData(platform, location, hash, dateAdded, productVersion);
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
