// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.oracle.weblogic.imagetool.settings.ConfigManager;
import org.junit.jupiter.api.io.TempDir;

public class TestSetup {
    public static void setup(@TempDir Path tempDir) throws IOException {
        Path settingsFileName = tempDir.resolve("settings.yaml");
        Path installerFile = tempDir.resolve("installers.yaml");
        Path patchFile = tempDir.resolve("patches.yaml");
        Files.createFile(settingsFileName);
        Files.createFile(installerFile);
        Files.createFile(patchFile);


        List<String> lines = Arrays.asList(
            "installerSettingsFile: " + installerFile.toAbsolutePath().toString(),
            "patchSettingsFile: " + patchFile.toAbsolutePath().toString(),
            "installerDirectory: " + tempDir.toAbsolutePath().toString(),
            "patchDirectory: " + tempDir.toAbsolutePath().toString()
        );
        Files.write(settingsFileName, lines);
        ConfigManager.getInstance(settingsFileName);

    }
}
