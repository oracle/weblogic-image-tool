// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        sortOptions = false
)
public class AddPatchEntry extends CacheOperation {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AddPatchEntry.class);

    @Override
    public CommandResponse call() throws Exception {

        if (type != null) {
            logger.warning("[[cyan: --type]] is [[brightred: DEPRECATED]] and will be removed in an upcoming release.");
        }

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
    private CommandResponse addToCache(String patchNumber) throws CacheStoreException {
        if (cache().getValueFromCache(patchNumber) != null) {
            return new CommandResponse(-1, "IMG-0076", patchNumber);
        }
        cache().addToCache(patchNumber, location.toAbsolutePath().toString());
        return new CommandResponse(0, Utils.getMessage("IMG-0075", patchNumber, location.toAbsolutePath()));
    }

    @Option(
            names = {"--patchId"},
            description = "Patch number. Ex: 28186730",
            required = true
    )
    private String patchId;

    @Option(
            names = {"--type"},
            description = "Type of patch. DEPRECATED"
    )
    @Deprecated
    private String type;

    @Option(
            names = {"--path"},
            description = "Location on disk. For ex: /path/to/FMW/patch.zip",
            required = true
    )
    private Path location;

}
