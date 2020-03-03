// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
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
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Base class to represent either an installer or a patch file.
 */
public class CachedFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);

    private String key;

    /**
     * Represents a locally cached file.
     *
     * @param id          cache ID (like installer type or patchId)
     * @param version     version number for the patch or installer.
     */
    public CachedFile(String id, String version) {
        Objects.requireNonNull(id, "key for the cached file cannot be null");
        key = generateKey(id, version);
    }

    /**
     * Represents a locally cached file.
     *
     * @param id          cache ID (like installer type)
     * @param version     version number for the patch or installer.
     */
    public CachedFile(InstallerType id, String version) {
        this(id.toString(), version);
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    private String generateKey(String id, String version) {
        logger.entering(id, version);
        String mykey = id;
        // if the patch id does not have the version suffix, add it here (like 111111_12.2.1.3.0)
        if (id.indexOf('_') < 0) {
            if (version != null) {
                mykey = mykey + CacheStore.CACHE_KEY_SEPARATOR + version;
            }
        }

        logger.exiting(mykey);
        return mykey;
    }

    public String getKey() {
        return key;
    }

    /**
     * Get the path of the file stored locally in the cache.
     * @param cacheStore the cache store to search
     * @return the Path of the file, if found
     * @throws IOException throws FileNotFoundException, if this cached file (key) could not be located in the cache
     */
    public String resolve(CacheStore cacheStore) throws IOException {
        // check entry exists in cache
        String key = getKey();
        logger.entering(key);
        String filePath = cacheStore.getValueFromCache(key);
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
        logger.entering();
        Path result = null;
        String sourceFile = resolve(cacheStore);
        logger.info("IMG-0043", sourceFile);
        String targetFilename = new File(sourceFile).getName();
        try {
            result = Files.copy(Paths.get(sourceFile), Paths.get(buildContextDir, targetFilename));
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        logger.exiting(result);
        return result;
    }
}
