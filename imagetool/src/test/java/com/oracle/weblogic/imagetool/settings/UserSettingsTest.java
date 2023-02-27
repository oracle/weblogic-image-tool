// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.cli.config.ConfigAttributeName;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
class UserSettingsTest {

    private UserSettingsFile getResourceFile(String resourcePath) {
        URL resource = this.getClass().getClassLoader().getResource(resourcePath);
        assertNotNull(resource, "Unable to load settings file: " + resourcePath);
        try {
            return new UserSettingsFile(Paths.get(resource.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Yaml resource file unavailable", e);
        }
    }

    @Test
    void testSimpleSettingsFile() {
        UserSettingsFile settings = getResourceFile("settings/basic_settings.yaml");
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
        UserSettingsFile settings = getResourceFile("settings/basic_settings.yaml");
        assertEquals("8u241", settings.getInstallerSettings(InstallerType.JDK).getDefaultVersion());
        assertEquals("12.2.1.4.0", settings.getInstallerSettings(InstallerType.WLS).getDefaultVersion());
    }

    @Test
    void testInvalidSettings() {
        UserSettingsFile settings = getResourceFile("settings/invalid_settings.yaml");
        assertEquals("/home/user/patches", settings.getPatchDirectory());
        assertNull(settings.getBuildContextDirectory());
    }

    @Test
    void testOutput() {
        String expected = "aruRetryInterval: 200\n"
            + "buildContextDirectory: ./builds\n"
            + "patchDirectory: /home/user/patches\n";

        UserSettingsFile settings = getResourceFile("settings/basic_settings.yaml");
        assertEquals(expected, settings.toString());
    }

    @Test
    void testSetters() {
        UserSettingsFile settings = getResourceFile("settings/basic_settings.yaml");
        ConfigAttributeName attributeName = ConfigAttributeName.patchDirectory;
        attributeName.set(settings, "./cache/paches");
        assertEquals("./builds", settings.getBuildContextDirectory());
        assertEquals("./cache/paches", settings.getPatchDirectory());
    }
}