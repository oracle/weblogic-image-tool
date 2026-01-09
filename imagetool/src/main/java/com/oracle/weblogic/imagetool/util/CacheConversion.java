// Copyright (c) 2025, 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cli.cache.CacheOperation;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import picocli.CommandLine;

/**
 * Utility to convert image tool 1.x cache store to 2.0 format.
 */
@CommandLine.Command(
    name = "convert",
    description = "Convert cache settings from v1 to v2",
    sortOptions = false
)
public class CacheConversion extends CacheOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CacheConversion.class);

    private boolean initializeSettingFiles(String directory) {
        boolean success = true;
        logger.entering("Entering initializeSettingFiles " + directory);
        try {

            if (directory != null) {
                Path directoryPath = Paths.get(directory);
                if (!Files.exists(directoryPath)) {
                    createIfNotExists(directoryPath, true);
                }

                Path settingsPath = directoryPath.resolve("settings.yaml");
                if (!Files.exists(settingsPath)) {
                    createNewSettingsYaml(settingsPath);
                }
            }
        } catch (Exception ex) {
            logger.warning("IMG-0138", ex.getLocalizedMessage());
            return false;
        }
        logger.exiting("initializeSettingFiles");
        return success;
    }

    private void createNewSettingsYaml(Path settingsPath) throws IOException {
        logger.fine("No existing settings file creating it");
        createIfNotExists(settingsPath, false);
        Path parentPath = settingsPath.getParent();
        List<String> lines = new ArrayList<>();
        lines.add("installerDirectory: " + parentPath.resolve("installers").toString());
        lines.add("patchDirectory: " + parentPath.resolve("patches").toString());
        createIfNotExists(parentPath.resolve("installers"), true);
        createIfNotExists(parentPath.resolve("patches"), true);
        createIfNotExists(parentPath.resolve("installers.yaml"), false);
        createIfNotExists(parentPath.resolve("patches.yaml"), false);

        Files.write(settingsPath, lines);
    }

    private void createIfNotExists(Path entry, boolean isDir) throws IOException {
        if (Files.exists(entry)) {
            return;
        }
        if (isDir) {
            Files.createDirectory(entry);
        } else {
            Files.createFile(entry);
        }
    }

    @Override
    public CommandResponse call() throws Exception {
        String cacheEnv = System.getenv(CacheStore.CACHE_DIR_ENV);
        Path cacheMetaFile;
        if (cacheEnv != null) {
            cacheMetaFile = Paths.get(cacheEnv, ".metadata");
            if (!initializeSettingFiles(cacheMetaFile.getParent().toString())) {
                return CommandResponse.error("IMG-0139");
            }
        } else {
            cacheMetaFile = Paths.get(System.getProperty("user.home"), "cache", ".metadata");
            if (!initializeSettingFiles(Paths.get(System.getProperty("user.home"), ".imagetool").toString())) {
                return CommandResponse.error("IMG-0139");
            }
        }
        if (Files.exists(cacheMetaFile)) {
            CacheConverterUtil.convert(cacheMetaFile.toString());
        } else {
            logger.info("IMG-0133", cacheMetaFile.toString());
        }
        return CommandResponse.success("IMG-0134");
    }

}
