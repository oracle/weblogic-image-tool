// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.patch;

import java.util.Objects;

import com.oracle.weblogic.imagetool.util.Utils;

public class PatchMetaData {
    private String architecture;
    private String location;
    private String digest;
    private String dateAdded;
    private String patchVersion;
    private String description;

    /**
     * Constructor.
     * @param architecture platform
     * @param location file path of the patch
     * @param digest sha256 hash
     * @param dateAdded date added
     * @param patchVersion version
     */
    public PatchMetaData(String architecture, String location, String digest, String dateAdded, String patchVersion,
                         String description) {
        this.architecture = architecture;
        this.location = location;
        this.digest = digest;
        this.dateAdded = dateAdded;
        this.patchVersion = patchVersion;
        this.description = description;
    }

    /**
     * Constructor.
     * @param architecture platform
     * @param location file path of the patch
     * @param patchVersion version
     * @param description description of the patch
     */
    public PatchMetaData(String architecture, String location, String patchVersion, String description) {
        this.architecture = architecture;
        this.location = location;
        this.digest = Utils.getSha256Hash(location);
        this.dateAdded = Utils.getTodayDate();
        this.patchVersion = patchVersion;
        this.description = description;
    }

    /**
     * Constructor.
     * @param architecture platform
     * @param location file path of the patch
     * @param patchVersion version
     * @param description description of the patch
     * @param dateAdded date added
     */
    public PatchMetaData(String architecture, String location, String patchVersion, String description,
                         String dateAdded) {
        this.architecture = architecture;
        this.location = location;
        this.digest = Utils.getSha256Hash(location);
        this.dateAdded = dateAdded;
        this.patchVersion = patchVersion;
        this.description = description;
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getLocation() {
        return location;
    }

    public String getDigest() {
        return digest;
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
            + "platform='" + architecture + '\''
            + ", location='" + location + '\''
            + ", digest='" + digest + '\''
            + ", dateAdded='" + dateAdded + '\''
            + ", patchVersion='" + patchVersion + '\''
            + ", description='" + description + '\''
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PatchMetaData metaData = (PatchMetaData) o;
        return Objects.equals(architecture, metaData.architecture)
            && Objects.equals(location, metaData.location) && Objects.equals(digest, metaData.digest)
            && Objects.equals(dateAdded, metaData.dateAdded)
            && Objects.equals(patchVersion, metaData.patchVersion)
            && Objects.equals(description, metaData.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(architecture, location, digest, dateAdded, patchVersion, description);
    }
}
