// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruProduct;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InstalledPatch;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.aru.InvalidPatchNumberException;
import com.oracle.weblogic.imagetool.aru.MultiplePatchVersionsException;
import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.cachestore.PatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.InvalidPatchIdFormatException;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

public abstract class CommonPatchingOptions extends CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonPatchingOptions.class);

    abstract String getInstallerVersion();

    /**
     * Get the installer type selected by the user.
     * This method is overridden by UpdateImage to provide the installer type
     * from the fromImage.
     * @return WLS by default, or the value selected by the user on the command line.
     */
    FmwInstallerType getInstallerType() {
        return installerType;
    }

    boolean isInstallerTypeSet() {
        return isOptionSet("--type");
    }

    @Override
    void initializeOptions() throws IOException, InvalidCredentialException, InvalidPatchIdFormatException {
        super.initializeOptions();
        password = Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);

        // if userid or password is provided, validate the pair of provided values
        if ((userId != null || password != null) && !AruUtil.rest().checkCredentials(userId, password)) {
            throw new InvalidCredentialException();
        }

        Utils.validatePatchIds(patches, false);
    }

    /**
     * Returns true if any patches should be applied.
     * A PSU is considered a patch.
     *
     * @return true if applying patches
     */
    boolean applyingPatches() {
        return (latestPsu || recommendedPatches) || !patches.isEmpty();
    }

    /**
     * Returns true if latestPsu or recommendedPatches was requested.
     *
     * @return true if applying patches
     */
    boolean applyingRecommendedPatches() {
        return (latestPsu || recommendedPatches);
    }

    /**
     * Should OPatch version be updated.
     * OPatch should be updated to the latest version available unless the user
     * requests that OPatch should not be updated.
     * @return true if OPatch should be updated.
     */
    boolean shouldUpdateOpatch() {
        if (skipOpatchUpdate) {
            logger.fine("IMG-0065");
        }
        return !skipOpatchUpdate;
    }

    /**
     * Process all patches requested by the user, if any.
     * Downloads and copies patch JARs to the build context directory.
     *
     * @throws AruException     if an error occurs trying to read patch metadata from ARU.
     * @throws IOException      if a transport error occurs trying to access the Oracle REST services.
     * @throws XPathExpressionException when the payload from the REST service is not formatted as expected
     *                          or a partial response was returned.
     */
    void handlePatchFiles() throws AruException, XPathExpressionException, IOException {
        if (dockerfileOptions.installMiddleware()) {
            handlePatchFiles(Collections.emptyList());
        } else {
            if (applyingPatches()) {
                // user intended to apply patches, but used a fromImage that already had an Oracle Home
                // The template does not support (yet) patches on existing Oracle Homes due to image size impact
                logger.warning("IMG-0093");
            }
        }
    }

    /**
     * Process all patches requested by the user, if any.
     * Downloads and copies patch JARs to the build context directory.
     *
     * @param installedPatches  a list of patches applied already installed on the target image.
     * @throws AruException     if an error occurs trying to read patch metadata from ARU.
     * @throws IOException      if a transport error occurs trying to access the Oracle REST services.
     * @throws XPathExpressionException when the payload from the REST service is not formatted as expected
     *                          or a partial response was returned.
     */
    void handlePatchFiles(List<InstalledPatch> installedPatches)
        throws AruException, IOException, XPathExpressionException {
        logger.entering(getInstallerType(), installedPatches);
        if (!applyingPatches()) {
            logger.exiting("not applying patches");
            return;
        }
        String psuVersion = InstalledPatch.getPsuVersion(installedPatches);

        List<AruPatch> aruPatches = getRecommendedPatchList();

        if (!aruPatches.isEmpty()) {
            // when applying a new PSU, use that PSU version to find patches where user did not qualify the patch number
            for (AruPatch patch : aruPatches) {
                if (patch.isPsu() && AruProduct.fromProductId(patch.product()) == AruProduct.WLS) {
                    String psuName = patch.psuBundle();
                    String psu = psuName.substring(psuName.lastIndexOf(' ') + 1);
                    logger.fine("Using PSU {0} to set preferred patch version to {1}", patch.patchId(), psu);
                    psuVersion = psu;
                }
            }
        }

        // add user-provided patch list to any patches that were found for latestPsu or recommendedPatches
        for (String patchId : patches) {
            // if user mistakenly added the OPatch patch to the WLS patch list, skip it. WIT updates OPatch anyway
            if (OPatchFile.isOPatchPatch(patchId)) {
                continue;
            }
            // if patch ID was provided as bugnumber_version, split the bugnumber and version strings
            String providedVersion = null;
            int split = patchId.indexOf('_');
            if (split > 0) {
                providedVersion = patchId.substring(split + 1);
                patchId = patchId.substring(0, split);
            }
            List<AruPatch> patchVersions = AruUtil.rest().getPatches(patchId, userId, password);

            // Stack Patch Bundle (SPB) is not a traditional patch.  Patches in SPB are duplicates of recommended.
            if (patchVersions.stream().anyMatch(AruPatch::isStackPatchBundle)) {
                // Do not continue if the user specified a patch number that cannot be applied.
                throw logger.throwing(new InvalidPatchNumberException(Utils.getMessage("IMG-0098", patchId)));
            }

            if (!patchVersions.isEmpty()) {
                AruPatch selectedVersion = AruPatch.selectPatch(patchVersions, providedVersion, psuVersion,
                    getInstallerVersion());

                if (selectedVersion != null) {
                    aruPatches.add(selectedVersion);
                } else {
                    throw logger.throwing(new MultiplePatchVersionsException(patchId, aruPatches));
                }
            }
        }

        AruUtil.validatePatches(installedPatches, aruPatches, userId, password);

        String patchesFolderName = createPatchesTempDirectory().toAbsolutePath().toString();
        // copy the patch JARs to the Docker build context directory from the local cache, downloading them if needed
        for (AruPatch patch : aruPatches) {
            PatchFile patchFile = new PatchFile(patch, userId, password);
            String patchLocation = patchFile.resolve(cache());
            if (patchLocation != null && !Utils.isEmptyString(patchLocation)) {
                File cacheFile = new File(patchLocation);
                try {
                    if (patch.fileName() == null) {
                        patch.fileName(cacheFile.getName());
                    }
                    Files.copy(Paths.get(patchLocation), Paths.get(patchesFolderName, cacheFile.getName()));
                } catch (FileAlreadyExistsException ee) {
                    logger.warning("IMG-0077", patchFile.getKey());
                }
            } else {
                logger.severe("IMG-0024", patchFile.getKey());
            }
        }
        if (!aruPatches.isEmpty()) {
            dockerfileOptions
                .setPatchingEnabled()
                .setStrictPatchOrdering(strictPatchOrdering)
                .setPatchList(aruPatches);
        }
        logger.exiting();
    }

    /**
     * Get all the latest PSU patches for a given installer type (WLS, SOA, etc.) if the user
     * requested them with --latestPSU.  --recommendedPatches takes precedence over --latestPSU, and
     * returns all the recommended patches including the PSUs.
     *
     * @return recommended patch list or empty list if neither latestPSU nor recommendedPatches was requested
     *         by the user.
     */
    List<AruPatch> getRecommendedPatchList() throws AruException {
        // returned List object should be modifiable
        List<AruPatch> aruPatches = new ArrayList<>();
        if (!applyingRecommendedPatches()) {
            return aruPatches;
        }

        if (userId == null || password == null) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0031"));
        }

        if (recommendedPatches) {
            // Get the latest PSU and its recommended patches
            aruPatches = AruUtil.rest()
                .getRecommendedPatches(getInstallerType(), getInstallerVersion(), userId, password);

            if (aruPatches.isEmpty()) {
                recommendedPatches = false;
                logger.info("IMG-0084", getInstallerVersion());
            } else if (FmwInstallerType.isBaseWeblogicServer(getInstallerType())) {
                // find and remove all ADR patches in the recommended patches list for base WLS installers
                List<AruPatch> discard = aruPatches.stream()
                    .filter(p -> p.description().startsWith("ADR FOR WEBLOGIC SERVER"))
                    .collect(Collectors.toList());
                // let the user know that the ADR patches will be discarded
                discard.forEach(p -> logger.info("IMG-0085", p.patchId()));
                aruPatches.removeAll(discard);
            }
        } else if (latestPsu) {
            // PSUs for WLS and JRF installers are considered WLS patches
            aruPatches = AruUtil.rest().getLatestPsu(getInstallerType(), getInstallerVersion(), userId, password);

            if (aruPatches.isEmpty()) {
                latestPsu = false;
                logger.fine("Latest PSU NOT FOUND, ignoring latestPSU flag");
            }
        }

        return aruPatches;
    }

    private Path createPatchesTempDirectory() throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(buildDir(), "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }

    void prepareOpatchInstaller(String tmpDir, String opatchBugNumber)
        throws IOException, XPathExpressionException, AruException {
        logger.entering(opatchBugNumber);
        String filePath = OPatchFile.getInstance(opatchBugNumber, userId, password, cache()).resolve(cache());
        String filename = new File(filePath).getName();
        Files.copy(Paths.get(filePath), Paths.get(tmpDir, filename));
        dockerfileOptions.setOPatchPatchingEnabled();
        dockerfileOptions.setOPatchFileName(filename);
        logger.exiting(filename);
    }

    String getUserId() {
        return userId;
    }

    String getPassword() {
        return password;
    }


    @Option(
        names = {"--user"},
        paramLabel = "<support email>",
        description = "Oracle Support email id"
    )
    private String userId;

    private String password;

    @Option(
        names = {"--password"},
        interactive = true,
        arity = "0..1",
        paramLabel = "<support password>",
        description = "Enter password for Oracle Support userId on STDIN"
    )
    private String passwordStr;

    @Option(
        names = {"--passwordEnv"},
        paramLabel = "<environment variable>",
        description = "environment variable containing the support password"
    )
    private String passwordEnv;

    @Option(
        names = {"--passwordFile"},
        paramLabel = "<password file>",
        description = "path to file containing just the password"
    )
    private Path passwordFile;

    @Option(
        names = {"--latestPSU"},
        description = "Whether to apply patches from latest PSU."
    )
    private boolean latestPsu = false;

    @Option(
        names = {"--recommendedPatches"},
        description = "Whether to apply recommended patches from latest PSU."
    )
    private boolean recommendedPatches = false;

    @Option(
        names = {"--strictPatchOrdering"},
        description = "Use OPatch to apply patches one at a time."
    )
    private boolean strictPatchOrdering = false;

    @Option(
        names = {"--patches"},
        paramLabel = "patchId",
        split = ",",
        description = "Comma separated patch Ids. Ex: 12345678,87654321"
    )
    List<String> patches = new ArrayList<>();

    @Option(
        names = {"--opatchBugNumber"},
        description = "the patch number for OPatch (patching OPatch)"
    )
    String opatchBugNumber;

    @Option(
        names = {"--skipOpatchUpdate"},
        description = "Do not update OPatch version, even if a newer version is available."
    )
    private boolean skipOpatchUpdate = false;

    @Option(
        names = {"--type"},
        description = "Installer type. Default: WLS. Supported values: ${COMPLETION-CANDIDATES}"
    )
    private FmwInstallerType installerType = FmwInstallerType.WLS;
}
