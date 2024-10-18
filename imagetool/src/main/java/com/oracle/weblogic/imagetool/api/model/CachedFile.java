// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.installer.InstallerMetaData;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Base class to represent either an installer or a patch file.
 */
public class CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);

    private final String version;
    private final String architecture;
    private final InstallerType installerType;
    private final UserSettingsFile userSettingsFile = new UserSettingsFile();
    private final boolean isPatch;
    private final String patchId;

    /**
     * Represents a locally cached file.
     *
     * @param id           cache ID (like installer type)
     * @param version      version number for the patch or installer.
     * @param architecture the system architecture that this file/installer is applicable
     */
    public CachedFile(InstallerType id, String version, String architecture) {
        this.installerType = id;
        this.version = version;
        this.architecture = architecture;
        this.isPatch = false;
        this.patchId = null;
    }

    /**
     * Represents a locally cached file.
     *
     * @param id          cache ID (like installer type)
     * @param version     version number for the patch or installer.
     */
    public CachedFile(InstallerType id, String version) {
        this(id, version, null);
    }

    /**
     * constructor.
     * @param isPatch is it a patch
     * @param patchId patch id
     * @param version version
     * @param architecture architecture
     */
    public CachedFile(boolean isPatch, String patchId, String version, String architecture) {
        this.isPatch = isPatch;
        this.version = version;
        this.architecture = architecture;
        this.installerType = null;
        this.patchId = patchId;
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    public String getPatchId() {
        return patchId;
    }

    public UserSettingsFile getUserSettingsFile() {
        return userSettingsFile;
    }

    /**
     * Get the version number for this cache entry/file.
     * @return the string version of this cached file.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the system architecture name for this cache entry/file.
     * @return the system architecture name applicable fo this cached file.
     */
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Get the path of the file stored locally in the cache.
     * Searching the cache starts with the specified key.  If the key is not found in the cache,
     * one additional attempt is made to find an acceptable alternative.  The second search is based on
     * whether the user specified a platform/architecture.  If the user specified an architecture, check the cache
     * for an entry listing without the architecture in the key (generic architecture entry).  If the user
     * did not specify an architecture, check the cache for an entry listing using the local architecture
     * in case the user added the cache entry with the architecture.
     * @return the Path of the file, if found
     * @throws IOException throws FileNotFoundException, if this cached file (key) could not be located in the cache
     */
    public String resolve() throws IOException {
        // check entry exists in cache
        logger.entering();
        String filePath = null;

        InstallerMetaData metaData = userSettingsFile.getInstallerForPlatform(installerType, getArchitecture(),
            getVersion());
        if (metaData != null) {
            filePath = metaData.getLocation();
        }

        if (!isFileOnDisk(filePath)) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0011", filePath));
        }

        logger.exiting(filePath);
        return filePath;
    }

    /**
     * Copy file from cacheStore to Docker build context directory.
     * @param cacheStore cache to copy file from
     * @param buildContextDir directory to copy file to
     * @return the path of the file copied to the Docker build context directory
     */
    public Path copyFile(CacheStore cacheStore, String buildContextDir) throws IOException {
        logger.entering(installerType, version, architecture, buildContextDir);
        Path result;
        String sourceFile = resolve();
        logger.info("IMG-0043", sourceFile);
        String targetFilename = new File(sourceFile).getName();
        try {
            result = Files.copy(Paths.get(sourceFile), Paths.get(buildContextDir, targetFilename));
        } catch (Exception ee) {
            String msg = Utils.getMessage("IMG-0064", sourceFile, buildContextDir);
            logger.severe(msg);
            logger.fine(msg, ee);
            throw ee;
        }
        logger.exiting(result);
        return result;
    }
}
