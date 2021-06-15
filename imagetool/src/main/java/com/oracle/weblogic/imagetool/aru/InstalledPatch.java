// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

public class InstalledPatch {
    private static final LoggingFacade logger = LoggingFactory.getLogger(InstalledPatch.class);

    private String bugNumber;
    private String uniquePatchNumber;
    private String patchDescription;

    /**
     * Parse the output from the image probe list of Oracle patches.
     *
     * @param oraclePatches semi-colon separated list of patches and fields
     * @return a simple list of InstalledPatch
     */
    public static List<InstalledPatch> getPatchList(String oraclePatches) {
        List<InstalledPatch> result = new ArrayList<>();
        String[] tokens = oraclePatches.split(";");
        if (tokens.length % 3 != 0) {
            logger.severe("Too many tokens in oracle patches " + tokens.length);
        }
        for (int i = 0; i < tokens.length; i++) {
            InstalledPatch found = new InstalledPatch();
            found.bugNumber = tokens[i];
            if (tokens.length > i + 1) {
                found.uniquePatchNumber = tokens[++i];
            }
            if (tokens.length > i + 1) {
                found.patchDescription = tokens[++i].replaceAll("^\"|\"$", "");
            }
            result.add(found);
        }

        return result;
    }

    /**
     * Parse the patch descriptions and return the PSU number if there is one installed.
     * @param installedPatches The opatch lsinventory patches.
     * @return the version of the PSU, or null if no PSU is found.
     */
    public static String getPsuVersion(List<InstalledPatch> installedPatches) {
        String result = null;
        // search inventory for PSU and extract PSU version, if available
        Pattern patternOne = Pattern.compile(
            "WLS PATCH SET UPDATE (\\d+\\.\\d+\\.\\d+\\.\\d+\\.)\\d+\\(ID:(\\d+)\\.\\d+\\)");
        Pattern patternTwo = Pattern.compile(
            "WLS PATCH SET UPDATE (\\d+\\.\\d+\\.\\d+\\.\\d+\\.[1-9]\\d+)");

        for (InstalledPatch patch : installedPatches) {
            String description = patch.getPatchDescription();
            Matcher matchPatternOne = patternOne.matcher(description);
            Matcher matchPatternTwo = patternTwo.matcher(description);
            if (matchPatternOne.find()) {
                result = matchPatternOne.group(1) + matchPatternOne.group(2);
                logger.fine("Found PSU in inventory {0}, in {1}", result, description);
                break;
            } else if (matchPatternTwo.find()) {
                result = matchPatternTwo.group(1);
                logger.fine("Found PSU in inventory {0}, in {1}", result, description);
                break;
            }
        }
        return result;
    }

    public String getBugNumber() {
        return bugNumber;
    }

    public String getUniquePatchNumber() {
        return uniquePatchNumber;
    }

    public String getPatchDescription() {
        return patchDescription;
    }

    @Override
    public String toString() {
        return bugNumber + ":" + uniquePatchNumber + ":" + patchDescription;
    }
}
