// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.util.Map;

public class InstallerSettings {
    private String defaultVersion;

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
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }
}
