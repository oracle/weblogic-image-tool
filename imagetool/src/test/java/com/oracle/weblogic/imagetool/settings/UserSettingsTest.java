// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.InputStream;

import com.oracle.weblogic.imagetool.cli.config.ConfigAttributeName;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
class UserSettingsTest {

    private UserSettings getResourceFile(String resourcePath) {
        InputStream inputStream = this.getClass()
            .getClassLoader()
            .getResourceAsStream("settings/basic_settings.yaml");
        return UserSettings.load(inputStream);
    }

    @Test
    void testSimpleSettingsFile() {
        UserSettings settings = getResourceFile("settings/basic_settings.yaml");
        assertEquals("/home/user/patches", settings.getPatchDirectory());
        assertEquals("./builds", settings.getBuildContextDirectory());
        assertNull(settings.getBuildEngine());
        assertNull(settings.getContainerEngine());
        assertNull(settings.getAruRetryMax());
        assertEquals(200, settings.getAruRetryInterval());
        // value not set, should return default value
        assertNull(settings.getInstallerDirectory());
    }

    @Test
    void testDefaultInstallers() {
        UserSettings settings = getResourceFile("settings/basic_settings.yaml");
        assertEquals("8u241", settings.getDefaultInstallerVersion(InstallerType.JDK));
        assertEquals("12.2.1.4.0", settings.getDefaultInstallerVersion(InstallerType.WLS));
    }

    @Test
    void testInvalidSettings() {
        UserSettings settings = getResourceFile("settings/invalid_settings.yaml");
        assertEquals("/home/user/patches", settings.getPatchDirectory());
        assertEquals("./builds", settings.getBuildContextDirectory());
    }

    @Test
    void testOutput() {
        String expected = "aruRetryInterval: 200\n"
            + "buildContextDirectory: ./builds\n"
            + "installers:\n"
            + "  jdk:\n"
            + "    defaultVersion: 8u241\n"
            + "  wls:\n"
            + "    defaultVersion: 12.2.1.4.0\n"
            + "  wdt:\n"
            + "    defaultVersion: latest\n"
            + "patchDirectory: /home/user/patches\n";

        UserSettings settings = getResourceFile("settings/basic_settings.yaml");
        assertEquals(expected, settings.toYamlString());
    }

    @Test
    void testSetters() {
        UserSettings settings = getResourceFile("settings/basic_settings.yaml");
        ConfigAttributeName attributeName = ConfigAttributeName.patchDirectory;
        attributeName.set(settings, "./cache/paches");
        assertEquals("./builds", settings.getBuildContextDirectory());
        assertEquals("./cache/paches", settings.getPatchDirectory());
    }
}