// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        sortOptions = false
)
public class AddPatchEntry extends CacheOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AddPatchEntry.class);

    public AddPatchEntry() {
    }

    @Override
    public CommandResponse call() throws Exception {

        if (patchId != null && !patchId.isEmpty()
                && location != null && Files.exists(location)
                && Files.isRegularFile(location)) {

            List<String> patches = new ArrayList<>();
            patches.add(patchId);
            if (!Utils.validatePatchIds(patches, true)) {
                return new CommandResponse(-1, "Patch ID validation failed");
            }
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
     * Add patch to the cache.
     *
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
            key = key.substring(0, lastSeparator) + CacheStore.CACHE_KEY_SEPARATOR + Constants.OPATCH_PATCH_TYPE;
        }
        logger.info("adding key " + key);
        if (cacheStore.getValueFromCache(key) != null) {
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
