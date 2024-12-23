// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;


import com.oracle.weblogic.imagetool.settings.UserSettingsFile;

public enum ConfigAttributeName {
    buildContextDirectory("BuildContextDirectory") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setBuildContextDirectory(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getBuildContextDirectory();
        }
    },
    patchDirectory("PatchDirectory") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setPatchDirectory(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getPatchDirectory();
        }
    },
    installerDirectory("InstallerDirectory") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setInstallerDirectory(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getInstallerDirectory();
        }
    },
    buildEngine("BuildEngine") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setBuildEngine(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getBuildEngine();
        }
    },
    containerEngine("ContainerEngine") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setContainerEngine(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getContainerEngine();
        }
    },
    aruRetryMax("AruRetryMax") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setAruRetryMax(Integer.parseInt(value));
        }

        @Override
        public String get(UserSettingsFile settings) {
            //TODO check for null
            return settings.getAruRetryMax().toString();
        }
    },
    defaultBuildPlatform("DefaultBuildPlatform") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setDefaultBuildPlatform(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getDefaultBuildPlatform();
        }
    },
    installerSettingsFile("InstallerSettingsFile") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setInstallerDetailsFile(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getInstallerSettingsFile();
        }
    },
    patchSettingsFile("PatchSettingsFile") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setPatchSettingsFile(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getPatchSettingsFile();
        }
    },
    defaultWLSVersion("DefaultWLSVersion") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setDefaultWLSVersion(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getDefaultWLSVersion();
        }
    },
    defaultWDTVersion("DefaultWDTVersion") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setDefaultWDTVersion(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getDefaultWDTVersion();
        }
    },
    defaultJDKVersion("DefaultJDKVersion") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setDefaultJDKVersion(value);
        }

        @Override
        public String get(UserSettingsFile settings) {
            return settings.getDefaultJDKVersion();
        }
    },
    aruRetryInterval("AruRetryInterval") {
        @Override
        public void set(UserSettingsFile settings, String value) {
            settings.setAruRetryInterval(Integer.parseInt(value));
        }

        @Override
        public String get(UserSettingsFile settings) {
            //TODO check for null
            return settings.getAruRetryInterval().toString();
        }
    };

    private final String value;

    public abstract void set(UserSettingsFile settings, String value);

    public abstract String get(UserSettingsFile settings);

    ConfigAttributeName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
