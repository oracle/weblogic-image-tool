// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.jetbrains.annotations.Nullable;

import static com.oracle.weblogic.imagetool.aru.AruUtil.getAruPlatformId;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.ARCHITECTURE;
import static com.oracle.weblogic.imagetool.settings.YamlFileConstants.BASE_FMW_VERSION;
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


        Map<String, Object> allInstallers = new SettingsFile(
            Paths.get(ConfigManager.getInstance().getInstallerDetailsFile())).load();

        if (allInstallers == null) {
            return new HashMap<>();
        }

        Map<String, Map<String, List<InstallerMetaData>>> installerDetails = new HashMap<>();

        allInstallers.forEach((key, value) -> {
            if (key != null && !key.isEmpty()) {
                String upperKey = key.toUpperCase(); // Convert key to uppercase (e.g., jdk, wls, fmw)
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> installerValues = (Map<String, Object>) value;

                    Map<String, List<InstallerMetaData>> installerMetaData = new HashMap<>();

                    installerValues.forEach((individualKey, individualValue) -> {
                        List<InstallerMetaData> metaDataList = new ArrayList<>();

                        if (individualValue instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> installerList = (List<Object>) individualValue;
                            installerList.forEach(installerValue ->
                                metaDataList.add(createInstallerMetaData((Map<String, Object>) installerValue)));
                        } else {
                            metaDataList.add(createInstallerMetaData((Map<String, Object>) individualValue));
                        }

                        installerMetaData.put(individualKey, metaDataList);
                    });

                    installerDetails.put(upperKey, installerMetaData);

                } catch (IllegalArgumentException e) {
                    logger.warning("{0} could not be loaded: {1}", upperKey, InstallerType.class.getEnumConstants());
                }
            }
        });

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

        if (!baseFMWVersionExists(installerType, installerDetails, metaData.getBaseFMWVersion())) {
            logger.severe("IMG-0149", metaData.getBaseFMWVersion());
            System.exit(2);
        }

        if (!Utils.isBaseInstallerType(installerType) && metaData.getBaseFMWVersion() == null) {
            metaData.setBaseFMWVersion(metaData.getProductVersion());
        }

        // Update the list
        installerDetails.put(installerKey, installerMetaDataMap);
        saveAllInstallers(installerDetails);
    }

    private boolean baseFMWVersionExists(InstallerType type, Map<String,
        Map<String, List<InstallerMetaData>>> installerDetails, String baseFMWVersion) {

        if (!Utils.isBaseInstallerType(type)) {
            if (baseFMWVersion == null) {
                return true;
            } else {
                Map<String, List<InstallerMetaData>> installers = installerDetails.get(
                    InstallerType.WLS.toString().toUpperCase());
                return installers.containsKey(baseFMWVersion);
            }
        } else {
            return true;
        }
    }

    /**
     * Return the metadata for the platformed installer.
     * @param platformName platform name
     * @param installerVersion version of the installer
     * @return InstallerMetaData meta data for the installer
     */

    public InstallerMetaData getInstallerForPlatform(InstallerType installerType, Architecture platformName,
                                                     String installerVersion) {


        platformName = (platformName == null) ? Architecture.GENERIC : platformName;
        installerType = (installerType == null) ? InstallerType.WLS : installerType;
        
        String installerKey = installerType.toString().toUpperCase();
        installerVersion = verifyInstallerVersion(installerVersion, installerType);

        Map<String, List<InstallerMetaData>> installers = getInstallers().get(installerKey);
        if (installers != null && !installers.isEmpty()) {
            return getInstallerMetaData(installerVersion, installerType, platformName, installers);
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
            return getPatchMetaData(platformName, bugNumber, version, patches);
        }
        return null;
    }

    private static @Nullable PatchMetaData getPatchMetaData(String platformName, String bugNumber, String version,
                                                            Map<String, List<PatchMetaData>> patches) {

        List<PatchMetaData> patchMetaDataList = patches.get(bugNumber);
        if (patchMetaDataList == null || patchMetaDataList.isEmpty()) {
            return null;
        }

        for (PatchMetaData patchMetaData : patchMetaDataList) {
            boolean isPlatformMatch = (platformName == null || platformName.isEmpty())
                ? "Generic".equalsIgnoreCase(patchMetaData.getArchitecture())
                : platformName.equalsIgnoreCase(patchMetaData.getArchitecture());

            if (isPlatformMatch && patchMetaData.getPatchVersion().equals(version)) {
                return patchMetaData;
            }
        }

        // Fallback to search for "Generic" for OPatchFile's default bug number
        if (OPatchFile.DEFAULT_BUG_NUM.equals(bugNumber)) {
            return patchMetaDataList.stream()
                .filter(patchMetaData -> "Generic".equalsIgnoreCase(patchMetaData.getArchitecture()))
                .findFirst()
                .orElse(null);
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
        Map<String, Object> allPatches = new SettingsFile(
            Paths.get(ConfigManager.getInstance().getPatchDetailsFile())).load();

        if (allPatches == null || allPatches.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<PatchMetaData>> patchList = new HashMap<>();

        allPatches.forEach((key, value) -> {
            if (key != null) {
                List<PatchMetaData> patchMetaDataList = new ArrayList<>();

                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> valueList = (List<Object>) value;
                    valueList.forEach(item ->
                        patchMetaDataList.add(createPatchMetaData((Map<String, Object>) item)));
                } else {
                    patchMetaDataList.add(createPatchMetaData((Map<String, Object>) value));
                }

                patchList.put(key, patchMetaDataList);
            }
        });

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

    private String verifyInstallerVersion(String installerVersion, InstallerType installerType) {

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
        return installerVersion;
    }


    private InstallerMetaData getInstallerMetaData(String installerVersion, InstallerType installerType,
                                         Architecture platformName, Map<String, List<InstallerMetaData>> installers) {

        List<InstallerMetaData> installerMetaDataList = installers.get(installerVersion);

        if (installerMetaDataList != null && !installerMetaDataList.isEmpty()) {

            Optional<InstallerMetaData> foundInstaller = installerMetaDataList.stream()
                .filter(installerMetaData -> platformName.getAcceptableNames()
                    .contains(installerMetaData.getArchitecture()))
                .findFirst();

            if (foundInstaller.isPresent()) {
                return foundInstaller.get();
            }

            if (Utils.isGenericInstallerAcceptable(installerType)) {
                //If it can't find the specialized platform, try generic.

                foundInstaller = installerMetaDataList.stream()
                    .filter(installerMetaData -> Architecture.GENERIC.getAcceptableNames()
                            .contains(installerMetaData.getArchitecture()))
                        .findFirst();

                if (foundInstaller.isPresent()) {
                    return foundInstaller.get();
                }

            }
        }

        return null;
    }
}
