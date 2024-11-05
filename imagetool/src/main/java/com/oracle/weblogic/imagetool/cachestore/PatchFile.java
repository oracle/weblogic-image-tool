// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.patch.PatchMetaData;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import com.oracle.weblogic.imagetool.util.Utils;

import static com.oracle.weblogic.imagetool.util.Utils.getSha256Hash;
import static com.oracle.weblogic.imagetool.util.Utils.getTodayDate;

public class PatchFile {

    private static final LoggingFacade logger = LoggingFactory.getLogger(PatchFile.class);

    private final String userId;
    private final String password;
    private final AruPatch aruPatch;

    /**
     * Create an abstract file to hold the metadata for a patch file.
     *
     * @param userId   the username to use for retrieving the patch
     * @param password the password to use with the userId to retrieve the patch
     */
    public PatchFile(AruPatch aruPatch, String userId, String password) {
        this.aruPatch = aruPatch;
        this.userId = userId;
        this.password = password;
    }

    private boolean offlineMode() {
        return userId == null || password == null;
    }

    /**
     * download patch.
     * @param aruPatch patch
     * @param userSettingsFile settings
     * @return filname  path of the patch downloaded
     * @throws IOException error
     */
    public String downloadPatch(AruPatch aruPatch, UserSettingsFile userSettingsFile) throws IOException {
        String filename = AruUtil.rest().downloadAruPatch(aruPatch, userSettingsFile.getPatchDirectory(),
            userId, password);
        String hashString = getSha256Hash(filename);
        if (!hashString.equals(aruPatch.sha256Hash())) {
            throw new IOException("Patch file hash mismatch");
        }
        try {
            Map<String, List<PatchMetaData>> allPatches = userSettingsFile.getAllPatches();
            List<PatchMetaData> patches;
            if (allPatches.containsKey(aruPatch.patchId())) {
                patches = allPatches.get(aruPatch.patchId());
                patches.add(new PatchMetaData(aruPatch.platformName(),
                    filename,
                    aruPatch.sha256Hash(),
                    getTodayDate(),
                    aruPatch.version()));
                allPatches.remove(aruPatch.patchId());
                allPatches.put(aruPatch.patchId(),patches);
            } else {
                patches = new ArrayList<>();
                patches.add(new PatchMetaData(aruPatch.platformName(),
                    filename,
                    aruPatch.sha256Hash(),
                    getTodayDate(),
                    aruPatch.version()));
                allPatches.put(aruPatch.patchId(),patches);
            }
            userSettingsFile.saveAllPatches(allPatches, userSettingsFile.getPatchDetailsFile());

        } catch (Exception k) {
            k.printStackTrace();
        }
        return filename;
    }

    /**
     * Resolve the patch location.
     * @return path of the file
     * @throws IOException when file is not there
     */
    public String resolve() throws IOException {
        String cacheKey = aruPatch.patchId();
        logger.entering(cacheKey);

        String filePath = null;
        boolean fileExists = false;
        UserSettingsFile userSettingsFile = new UserSettingsFile();
        PatchMetaData patchSettings = userSettingsFile.getPatchForPlatform(aruPatch.platformName(), aruPatch.patchId());
        if (patchSettings != null) {
            filePath = patchSettings.getLocation();
            fileExists = isFileOnDisk(filePath);
        }

        if (fileExists) {
            logger.info("IMG-0017", cacheKey, filePath);
        } else {
            logger.info("IMG-0061", cacheKey, aruPatch.patchId());

            if (offlineMode()) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0056", cacheKey));
            }
            filePath = downloadPatch(aruPatch, userSettingsFile);
        }

        logger.exiting(filePath);
        return filePath;
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

}
