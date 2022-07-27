// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSettingsTest {

    @Test
    void testSimpleSettingsFile() {
        InputStream inputStream = this.getClass()
            .getClassLoader()
            .getResourceAsStream("settings/basic_settings.yaml");
        UserSettings settings = UserSettings.load(inputStream);
        assertEquals("/home/user/patches", settings.getPatchDirectory());
        assertEquals("./builds", settings.getImageBuildDirectory());
        assertEquals("docker", settings.getBuildEngine());
        assertEquals("docker", settings.getContainerEngine());
        assertEquals(10, settings.getAruRetryMax());
        assertEquals(200, settings.getAruRetryInterval());
    }

    @Test
    void testInvalidSettings() {
        InputStream inputStream = this.getClass()
            .getClassLoader()
            .getResourceAsStream("settings/invalid_settings.yaml");
        UserSettings settings = UserSettings.load(inputStream);
        assertEquals("/home/user/patches", settings.getPatchDirectory());
        assertEquals(".", settings.getImageBuildDirectory());
    }

    //@Test
    void testOutput() {
        //TODO: re-enable this test
        String expected = "aruRetryInterval: 200\n"
            + "imageBuildDirectory: ./builds\n"
            + "patchDirectory: /home/user/patches\n";

        InputStream inputStream = this.getClass()
            .getClassLoader()
            .getResourceAsStream("settings/basic_settings.yaml");
        UserSettings settings = UserSettings.load(inputStream);
        assertEquals(expected, settings.toYamlString());
    }
}