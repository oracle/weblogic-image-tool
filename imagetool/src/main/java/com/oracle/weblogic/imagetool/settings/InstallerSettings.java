// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.util.ArrayList;
import java.util.Map;

import com.oracle.weblogic.imagetool.installer.InstallerMetaData;

public class InstallerSettings {
    private String defaultVersion;
    private Map<String, Object> installerList;

    public InstallerSettings(Map<String,Object> settings) {
        applySettings(settings);
    }

    /**
     * Apply settings that were loaded from YAML.
     * @param settings map from YAML parser
     */
    private void applySettings(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return;
        }

        defaultVersion = SettingsFile.getValue("defaultVersion", String.class, settings);
        installerList = settings;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    /**
     * Test.
     * @param platformName platform name
     * @param version version
     * @return InstallerMetaData
     */
    public InstallerMetaData getInstallerForPlatform(String platformName, String version) {
        if (installerList != null && !installerList.isEmpty()) {
            // TODO: check version exists
            Object versionedInstaller = installerList.get(version);
            if (versionedInstaller instanceof ArrayList) {
                for (Object installerObj : (ArrayList) versionedInstaller) {
                    Map<String, Object> installerMap = (Map<String, Object>) installerObj;
                    String platform = (String) installerMap.get("platform");
                    if (platform.equals(platformName)) {
                        String hash = (String) installerMap.get("digest");
                        String dateAdded = (String) installerMap.get("added");
                        String location = (String) installerMap.get("file");
                        String versionName = (String) installerMap.get("version");
                        return new InstallerMetaData(platform, location, hash, dateAdded, versionName);
                    }
                }
            }
        }
        return null;
    }

}
