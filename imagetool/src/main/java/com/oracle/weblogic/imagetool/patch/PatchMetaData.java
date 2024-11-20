// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.patch;

import com.oracle.weblogic.imagetool.util.Utils;

public class PatchMetaData {
    private String platform;
    private String location;
    private String hash;
    private String dateAdded;
    private String patchVersion;
    private String description;

    /**
     * Constructor.
     * @param platform platform
     * @param location file path of the patch
     * @param hash sha256 hash
     * @param dateAdded date added
     * @param patchVersion version
     */
    public PatchMetaData(String platform, String location, String hash, String dateAdded, String patchVersion,
                         String description) {
        this.platform = platform;
        this.location = location;
        this.hash = hash;
        this.dateAdded = dateAdded;
        this.patchVersion = patchVersion;
        this.description = description;
    }

    /**
     * Constructor.
     * @param platform platform
     * @param location file path of the patch
     * @param patchVersion version
     */
    public PatchMetaData(String platform, String location, String patchVersion, String description) {
        this.platform = platform;
        this.location = location;
        this.hash = Utils.getSha256Hash(location);
        this.dateAdded = Utils.getTodayDate();
        this.patchVersion = patchVersion;
        this.description = description;
    }

    public String getPlatform() {
        return platform;
    }

    public String getLocation() {
        return location;
    }

    public String getHash() {
        return hash;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public String getPatchVersion() {
        return patchVersion;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "PatchMetaData{"
            + "platform='" + platform + '\''
            + ", location='" + location + '\''
            + ", hash='" + hash + '\''
            + ", dateAdded='" + dateAdded + '\''
            + ", patchVersion='" + patchVersion + '\''
            + ", description='" + description + '\''
            + '}';
    }
}
