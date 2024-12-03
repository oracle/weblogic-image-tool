// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class InstalledPatch {
    private static final LoggingFacade logger = LoggingFactory.getLogger(InstalledPatch.class);

    private String bugNumber;
    private String uniquePatchNumber;
    private String patchDescription;

    // Patterns for matching PSU versions in patch descriptions
    private static final Pattern PSU_VERSION_PATTERN = Pattern.compile(
        "WLS PATCH SET UPDATE (?:"
            + "(\\d+\\.\\d+\\.\\d+\\.\\d+\\.)\\d+\\(ID:(\\d+)\\.\\d+\\)|"  // Pattern for format: x.x.x.x.0(ID:123456.0)
            + "(\\d+\\.\\d+\\.\\d+\\.\\d+\\.[1-9]\\d+)"                     // Pattern for format: x.x.x.x.nn
        + ")");

    /**
     * Parse the output from the image probe list of Oracle patches.
     *
     * @param oraclePatches semi-colon separated list of patches and fields
     * @return a simple list of InstalledPatch
     */
    public static List<InstalledPatch> getPatchList(String oraclePatches) {
        logger.entering(oraclePatches);
        List<InstalledPatch> result = new ArrayList<>();
        if (Utils.isEmptyString(oraclePatches)) {
            return result;
        }
        String[] tokens = oraclePatches.split(";");
        // Each patch record is made up of 3 fields.
        if (tokens.length % 3 != 0) {
            logger.severe("IMG-0095", tokens.length);
        }
        for (int j = 0; j < tokens.length; j = j + 3) {
            InstalledPatch found = new InstalledPatch();
            found.bugNumber = tokens[j];
            found.uniquePatchNumber = tokens[j + 1];
            found.patchDescription = tokens[j + 2].replaceAll("(^\")|(\"$)", "");
            result.add(found);
        }

        logger.exiting(result.size());
        return result;
    }

    /**
     * Parse the patch descriptions and return the PSU number if there is one installed.
     * @param installedPatches The opatch lsinventory patches.
     * @return the version of the PSU, or null if no PSU is found.
     */
    public static String getPsuVersion(List<InstalledPatch> installedPatches) {
        if (installedPatches == null || installedPatches.isEmpty()) {
            return null;
        }

        for (InstalledPatch patch : installedPatches) {
            String description = patch.patchDescription();
            Matcher matcher = PSU_VERSION_PATTERN.matcher(description);
            
            if (matcher.find()) {
                String psuVersion;
                if (matcher.group(1) != null) {
                    // Handle format: x.x.x.x.0(ID:123456.0)
                    psuVersion = matcher.group(1) + matcher.group(2);
                } else {
                    // Handle format: x.x.x.x.nn
                    psuVersion = matcher.group(3);
                }
                logger.fine("Found PSU in inventory {0}, in {1}", psuVersion, description);
                return psuVersion;
            }
        }
        return null;
    }

    public String bugNumber() {
        return bugNumber;
    }

    public String uniquePatchNumber() {
        return uniquePatchNumber;
    }

    public String patchDescription() {
        return patchDescription;
    }

    @Override
    public String toString() {
        return bugNumber + ":" + uniquePatchNumber + ":" + patchDescription;
    }
}
