// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.util.Architecture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigManagerTest {

    Path settingsPath = Paths.get("src/test/resources/settings/basic_settings.yaml");

    @Test
    void testBasicInstallerSettings() {
        ConfigManager cfgMgr = ConfigManager.getInstance(settingsPath);
        assertNotNull(cfgMgr);
        assertEquals(200, cfgMgr.getAruRetryInterval());
        assertEquals(10, cfgMgr.getAruRetryMax());
        assertEquals("12.2.1.4.0", cfgMgr.getDefaultWLSVersion());
    }

    @Test
    void testGettingInstallers() {
        ConfigManager cfgMgr = ConfigManager.getInstance(settingsPath);
        assertNotNull(cfgMgr);
        Map<String, Map<String, List<InstallerMetaData>>> installers = cfgMgr.getInstallers();
        assertNotNull(installers);
        assertEquals(4, installers.size());
        InstallerMetaData metaData = cfgMgr.getInstallerForPlatform(InstallerType.JDK, Architecture.AMD64,
            "8u241");
        assertNotNull(metaData);
        assertEquals("8u241", metaData.getProductVersion());
        metaData = cfgMgr.getInstallerForPlatform(InstallerType.JDK, Architecture.AMD64,
            "8u301");
        assertNull(metaData);
    }

    @Test
    void testGettingPatches() {
        ConfigManager cfgMgr = ConfigManager.getInstance(settingsPath);
        assertNotNull(cfgMgr);
        Map<String, List<PatchMetaData>> patches = cfgMgr.getAllPatches();
        assertNotNull(patches);
        assertEquals(2, patches.size());
        PatchMetaData metaData = cfgMgr.getPatchForPlatform("Generic", "37258699",
            "12.2.1.4.241107");
        assertNull(metaData);
        metaData = cfgMgr.getPatchForPlatform("Generic", "37258699",
            "12.2.1.4.0");
        assertNotNull(metaData);
        assertEquals("12.2.1.4.0", metaData.getPatchVersion());
        assertEquals("JDBC19.25 BUNDLE PATCH 12.2.1.4.241107", metaData.getDescription());
    }

}
