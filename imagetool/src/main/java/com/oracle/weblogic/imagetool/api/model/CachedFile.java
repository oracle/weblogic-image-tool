// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.BuildPlatform;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Base class to represent either an installer or a patch file.
 */
public class CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);

    private final String id;
    private final String version;
    private final String architecture;

    /**
     * Represents a locally cached file.
     *
     * @param id           cache ID (like installer type or patchId)
     * @param version      version number for the patch or installer.
     * @param architecture the system architecture that this file/installer is applicable
     */
    public CachedFile(String id, String version, String architecture) {
        Objects.requireNonNull(id, "key for the cached file cannot be null");
        logger.entering(id, version, architecture);
        this.id = id;
        this.version = version;
        this.architecture = architecture;
        logger.exiting();
    }

    /**
     * Represents a locally cached file.
     *
     * @param id          cache ID (like installer type or patchId)
     * @param version     version number for the patch or installer.
     */
    public CachedFile(String id, String version) {
        this(id, version, null);
    }

    /**
     * Represents a locally cached file.
     *
     * @param id           cache ID (like installer type)
     * @param version      version number for the patch or installer.
     * @param architecture the system architecture that this file/installer is applicable
     */
    public CachedFile(InstallerType id, String version, String architecture) {
        this(id.toString(), version, architecture);
    }

    /**
     * Represents a locally cached file.
     *
     * @param id          cache ID (like installer type)
     * @param version     version number for the patch or installer.
     */
    public CachedFile(InstallerType id, String version) {
        this(id.toString(), version, null);
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    /**
     * Get the key for this cache entry.
     * If the ID that was used to create this CachedFile object contains the separator (underscore),
     * then the key is the same as the ID.  Otherwise, the key is the ID plus version, like ID + "_" + version.
     * @return the key to use for this cache entry, like xxxx_yyyy.
     */
    public String getKey() {
        return getCacheKey(architecture);
    }

    private String getCacheKey(String architecture) {
        if (id.contains(CacheStore.CACHE_KEY_SEPARATOR)) {
            return id;
        }

        StringBuilder key = new StringBuilder(32);
        key.append(id);
        key.append(CacheStore.CACHE_KEY_SEPARATOR);
        key.append(version);
        if (architecture != null) {
            key.append(CacheStore.CACHE_KEY_SEPARATOR);
            key.append(architecture);
        }
        return key.toString();
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
     * @param cacheStore the cache store to search
     * @return the Path of the file, if found
     * @throws IOException throws FileNotFoundException, if this cached file (key) could not be located in the cache
     */
    public String resolve(CacheStore cacheStore) throws IOException {
        // check entry exists in cache
        String key = getKey();
        logger.entering(key);
        String filePath = cacheStore.getValueFromCache(key);
        if (filePath == null) {
            // The KEY for this CachedFile was not found in the local cache.
            logger.fine("Unable to find cache entry for {0}", key);
            String alternateKey;
            if (getArchitecture() == null) {
                // The user did not specify an architecture in the KEY and that key was not found in the cache.
                // Try adding the local architecture to the key, and look for that entry.
                alternateKey = getCacheKey(BuildPlatform.getPlatformName());
                logger.fine("Trying local architecture: {0}", alternateKey);
            } else {
                // The user specified an architecture in the KEY, but that key was not found.
                // Try removing the architecture from the key, and look for that entry.
                alternateKey = getCacheKey(null);
                logger.fine("Trying no-arch/generic architecture: {0}", alternateKey);
            }
            // second attempt to find a reasonable cache entry
            filePath = cacheStore.getValueFromCache(alternateKey);
        }

        if (!isFileOnDisk(filePath)) {
            throw new FileNotFoundException(Utils.getMessage("IMG-0011", key));
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
        logger.entering(id, version, architecture, buildContextDir);
        Path result;
        String sourceFile = resolve(cacheStore);
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
