// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.FileResolver;
import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.InstallerFile;
import com.oracle.weblogic.imagetool.impl.PatchFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.ValidationResult;

public class OptionsHelper {

    private static final LoggingFacade logger = LoggingFactory.getLogger(ImageOperation.class);

    boolean latestPSU;
    List<String> patches;
    String userId;
    String password;
    CachePolicy useCache;
    CacheStore cacheStore;
    DockerfileOptions dockerfileOptions;
    String installerVersion;
    WLSInstallerType installerType;
    String tempDirectory;

    OptionsHelper(boolean latestPSU, List<String> patches, String userId, String password, CachePolicy useCache,
                         CacheStore cacheStore, DockerfileOptions dockerfileOptions, WLSInstallerType installerType,
                  String installerVersion, String tempDirectory) {
        this.latestPSU = latestPSU;
        this.patches = patches;
        this.userId = userId;
        this.password = password;
        this.useCache = useCache;
        this.cacheStore = cacheStore;
        this.dockerfileOptions = dockerfileOptions;
        this.installerType = installerType;
        this.installerVersion = installerVersion;
        this.tempDirectory = tempDirectory;

    }

    /**
     * Returns true if any patches should be applied.
     * A PSU is considered a patch.
     * @return true if applying patches
     */
    boolean applyingPatches() {
        return latestPSU || !patches.isEmpty();
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     *
     * @return list of strings
     * @throws Exception in case of error
     */
    List<String> handlePatchFiles(String previousInventory) throws Exception {
        logger.entering();
        List<String> retVal = new LinkedList<>();

        if (!applyingPatches()) {
            return retVal;
        }

        String toPatchesPath = createPatchesTempDirectory().toAbsolutePath().toString();

        List<String> patchLocations = new LinkedList<>();

        List<String> patchList = new ArrayList<>(patches);

        if (latestPSU) {
            if (userId == null || password == null) {
                throw new Exception("No credentials provided. Cannot determine latestPSU");
            }

            // PSUs for WLS and JRF installers are considered WLS patches
            String patchId = ARUUtil.getLatestPSUNumber(WLSInstallerType.WLS, installerVersion, userId, password);
            if (Utils.isEmptyString(patchId)) {
                throw new Exception(String.format("Failed to find latest psu for product category %s, version %s",
                   installerType, installerVersion));
            }
            logger.fine("Found latest PSU {0}", patchId);
            FileResolver psuResolver = new PatchFile(useCache, installerType.toString(), installerVersion,
                patchId, userId, password);
            patchLocations.add(psuResolver.resolve(cacheStore));
            // Add PSU patch ID to the patchList for validation (conflict check)
            patchList.add(patchId);
        }

        logger.info("IMG-0012");
        ValidationResult validationResult = ARUUtil.validatePatches(previousInventory, patchList, userId, password);
        if (validationResult.isSuccess()) {
            logger.info("IMG-0006");
        } else {
            String error = validationResult.getErrorMessage();
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }

        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                patchLocations.add(new PatchFile(useCache, installerType.toString(), installerVersion,
                    patchId, userId, password).resolve(cacheStore));
            }
        }
        for (String patchLocation : patchLocations) {
            if (patchLocation != null) {
                File patchFile = new File(patchLocation);
                Files.copy(Paths.get(patchLocation), Paths.get(toPatchesPath, patchFile.getName()));
            } else {
                logger.severe("null entry in patchLocation");
            }
        }
        if (!patchLocations.isEmpty()) {
            dockerfileOptions.setPatchingEnabled();
        }
        logger.exiting(retVal.size());
        return retVal;
    }

    public Path createPatchesTempDirectory() throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(tempDirectory, "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }


    void addOPatch1394ToImage(String tmpDir, String opatchBugNumber) throws Exception {
        // opatch patch now is in the format #####_opatch in the cache store
        // So the version passing to the constructor of PatchFile is also "opatch".
        // since opatch releases is on it's own and there is not really a patch to opatch
        // and the version is embedded in the zip file version.txt

        String filePath =
            new PatchFile(useCache, Constants.OPATCH_PATCH_TYPE, Constants.OPATCH_PATCH_TYPE, opatchBugNumber,
                userId, password).resolve(cacheStore);
        String filename = new File(filePath).getName();
        Files.copy(Paths.get(filePath), Paths.get(tmpDir, filename));
        dockerfileOptions.setOPatchPatchingEnabled();
        dockerfileOptions.setOPatchFileName(filename);
    }



    /**
     * Builds a list of build args to pass on to docker with the required installer files.
     * And, copies the installers over to build context dir.
     *
     * @param tmpDir build context directory
     * @return list of build argument parameters for docker build
     * @throws Exception in case of error
     */
    List<String> handleInstallerFiles(String tmpDir, List<InstallerFile> requiredInstallers) throws Exception {

        logger.entering(tmpDir);
        List<String> retVal = new LinkedList<>();
        for (InstallerFile installerFile : requiredInstallers) {
            String targetFilePath = installerFile.resolve(cacheStore);
            logger.finer("copying targetFilePath: {0}", targetFilePath);
            String filename = new File(targetFilePath).getName();
            try {
                Files.copy(Paths.get(targetFilePath), Paths.get(tmpDir, filename));
                retVal.addAll(installerFile.getBuildArg(filename));
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        logger.exiting(retVal);
        return retVal;
    }


}
