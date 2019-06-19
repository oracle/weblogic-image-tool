/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.AbstractFile;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.SearchResult;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.XPathUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        sortOptions = false
)
public class AddPatchEntry extends CacheOperation {

    private final Logger logger = Logger.getLogger(AddPatchEntry.class.getName());

    public AddPatchEntry() {
    }

    public AddPatchEntry(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() throws Exception {

        if (patchId != null && !patchId.isEmpty()
                && location != null && Files.exists(location)
                && Files.isRegularFile(location)) {

            List<String> patches = new ArrayList<>();
            patches.add(patchId);
            Utils.validatePatchIds(patches, true);

            return addToCache(patchId);
        }

        String msg = "Invalid arguments";
        if (patchId == null || patchId.isEmpty()) {
            msg += " : --patchId was not supplied";
        }
        if (location == null || !Files.exists(location) || !Files.isRegularFile(location)) {
            msg += " : --path is invalid";
        }

        return new CommandResponse(-1, msg);
    }

    /**
     * Validate local patch file's digest against the digest stored in ARU.
     * @param patchNumber the ARU patch number without the 'p'
     * @param password the password to be used for the ARU query (Oracle Support credential)
     * @return true if the local file digest matches the digest stored in Oracle ARU
     * @throws Exception if the ARU call to get patch details failed
     */
    private CommandResponse validateAndAddToCache(String patchNumber, String password) throws Exception {
        return addToCache(patchNumber);
    }

    /**
     * Add patch to the cache.
     * @param patchNumber the patchId (minus the 'p') of the patch to add
     * @return CLI command response
     */
    private CommandResponse addToCache(String patchNumber) {
        logger.info("adding key " + patchNumber);
        String key = patchNumber;

        // Check if it is an Opatch patch
        String opatchNumber = Utils.getOpatchVersionFromZip(location.toAbsolutePath().toString());
        if (opatchNumber != null) {
            int lastSeparator = key.lastIndexOf(CacheStore.CACHE_KEY_SEPARATOR);
            key = key.substring(0,lastSeparator) + CacheStore.CACHE_KEY_SEPARATOR + Constants.OPATCH_PATCH_TYPE;
        }
        logger.info("adding key " + key);
        if (cacheStore.getValueFromCache(key) != null ) {
            String error = String.format("Cache key %s already exists, remove it first", key);
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }
        cacheStore.addToCache(key, location.toAbsolutePath().toString());
        String msg = String.format("Added Patch entry %s=%s for %s", key, location.toAbsolutePath(), type);
        logger.info(msg);
        return new CommandResponse(0, msg);
    }


    @Option(
            names = {"--patchId"},
            description = "Patch number. Ex: 28186730",
            required = true
    )
    private String patchId;

    @Option(
            names = {"--type"},
            description = "Type of patch. Valid values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private WLSInstallerType type;


    @Option(
            names = {"--path"},
            description = "Location on disk. For ex: /path/to/FMW/patch.zip",
            required = true
    )
    private Path location;

}
