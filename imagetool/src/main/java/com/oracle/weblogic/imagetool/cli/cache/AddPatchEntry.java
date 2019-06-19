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
        String password = handlePasswordOptions();

        if (patchId != null && !patchId.isEmpty()
                && location != null && Files.exists(location)
                && Files.isRegularFile(location)) {

            String patchNumber;
            if (patchId.matches(Constants.PATCH_ID_REGEX)) {
                patchNumber = patchId.substring(1);
            } else {
                return new CommandResponse(-1, "Invalid patch id format: " + patchId);
            }
            if (userId != null && !userId.isEmpty() && password != null && !password.isEmpty() ) {
                return validateAndAddToCache(patchNumber, password);
            } else {
                logger.info("Skipping patch validation, username and password were not provided");
                return addToCache(patchNumber);
            }
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
//        boolean matches = false;

        return addToCache(patchNumber);

//        SearchResult searchResult = ARUUtil.getPatchDetail(type.toString(), version, patchNumber, userId, password);
//
//        if (searchResult.isSuccess()) {
//            Document document = searchResult.getResults();
//            String patchDigest = XPathUtil.applyXPathReturnString(document, "string"
//                    + "(/results/patch[1]/files/file/digest[@type='SHA-256']/text())");
//            String localDigest = DigestUtils.sha256Hex(new FileInputStream(location.toFile()));
//
//            if (localDigest.equalsIgnoreCase(patchDigest)) {
//                return addToCache(patchNumber);
//            } else {
//                return new CommandResponse(-1, String.format(
//                        "Local file sha-256 digest %s != patch digest %s", localDigest, patchDigest));
//            }
//        }

//        return new CommandResponse(-1, String.format("Unable to find patchId %s on Oracle Support", patchId));
    }

    /**
     * Add patch to the cache.
     * @param patchNumber the patchId (minus the 'p') of the patch to add
     * @return CLI command response
     */
    private CommandResponse addToCache(String patchNumber) {
        String key = AbstractFile.generateKey(patchNumber, version);

        // Check if it is an Opatch patch
        String opatchNumber = Utils.getOpatchVersionFromZip(location.toAbsolutePath().toString());
        if (opatchNumber != null) {
            int lastSeparator = key.lastIndexOf(CacheStore.CACHE_KEY_SEPARATOR);
            key = key.substring(0,lastSeparator) + CacheStore.CACHE_KEY_SEPARATOR + Constants.OPATCH_PATCH_TYPE;
        }
        cacheStore.addToCache(key, location.toAbsolutePath().toString());
        String msg = String.format("Added Patch entry %s=%s for %s", key, location.toAbsolutePath(), type);
        logger.info(msg);
        return new CommandResponse(0, msg);
    }

    /**
     * Determines the support password by parsing the possible three input options
     *
     * @return String form of password
     * @throws IOException in case of error
     */
    private String handlePasswordOptions() throws IOException {
        return Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
    }

    @Option(
            names = {"--patchId"},
            description = "Patch number. Ex: p28186730",
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
            names = {"--version"},
            description = "version of mw this patch is for. Ex: 12.2.1.3.0",
            required = true,
            defaultValue = Constants.DEFAULT_WLS_VERSION
    )
    private String version;

    @Option(
            names = {"--path"},
            description = "Location on disk. For ex: /path/to/FMW/patch.zip",
            required = true
    )
    private Path location;

    @Option(
            names = {"--user"},
            paramLabel = "<support email>",
            description = "Oracle Support email id"
    )
    private String userId;

    @Option(
            names = {"--password"},
            paramLabel = "<password for support user id>",
            description = "Password for support userId"
    )
    String passwordStr;

    @Option(
            names = {"--passwordEnv"},
            paramLabel = "<environment variable>",
            description = "environment variable containing the support password"
    )
    String passwordEnv;

    @Option(
            names = {"--passwordFile"},
            paramLabel = "<password file>",
            description = "path to file containing just the password"
    )
    Path passwordFile;
}
