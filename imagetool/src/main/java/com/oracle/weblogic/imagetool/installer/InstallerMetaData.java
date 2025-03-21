// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;

public class InstallerMetaData {
    private String architecture;
    private String location;
    private String digest;
    private String dateAdded;
    private String productVersion;
    private String baseFMWVersion;

    /**
     * Constructor InstallerMetaData stores details about this installer.
     * @param architecture   platform linux/arm64, linux/amd64
     * @param location   file path of the installer
     * @param digest     sha256 hash value
     * @param dateAdded  date added
     * @param productVersion version of this installer
     * @param baseFMWVersion base WLS version used by this installer
     */
    public InstallerMetaData(String architecture, String location, String digest, String dateAdded,
                             String productVersion, String baseFMWVersion) {
        this.architecture = Utils.standardPlatform(architecture);
        this.location = location;
        this.digest = digest;
        this.dateAdded = dateAdded;
        this.productVersion = productVersion;
        this.baseFMWVersion = baseFMWVersion;
    }

    /**
     * Constructor InstallerMetaData stores details about this installer.
     * @param architecture   platform linux/arm64, linux/amd64
     * @param location   file path of the installer
     * @param productVersion real version of this installer
     */
    public InstallerMetaData(String architecture, String location, String productVersion, String baseFMWVersion) {
        this.architecture = Utils.standardPlatform(architecture);
        this.location = location;
        this.productVersion = productVersion;
        if (location != null && Files.exists(Paths.get(location))) {
            this.digest = Utils.getSha256Hash(location);
            this.dateAdded = Utils.getTodayDate();
        }
        this.baseFMWVersion = baseFMWVersion;
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

    public String getProductVersion() {
        return productVersion;
    }

    public String getBaseFMWVersion() {
        return baseFMWVersion;
    }

    public void setBaseFMWVersion(String baseFMWVersion) {
        this.baseFMWVersion = baseFMWVersion;
    }

    /**
     * Return standard platform name from the possible names.
     * @param platform input value to convert
     * @return standardized platform name
     */
    public String standardPlatform(String platform) {
        if (Architecture.AMD64.getAcceptableNames().contains(platform)) {
            return "linux/amd64";
        }
        if (Architecture.ARM64.getAcceptableNames().contains(platform)) {
            return "linux/arm64";
        }
        return "Generic";
    }

    @Override
    public String toString() {
        return "InstallerMetaData [platform=" + architecture + ", location=" + location + ", hash=" + digest + ", "
            + "dateAdded=" + dateAdded + ", version=" + productVersion + ", baseFMWVersion=" + baseFMWVersion
            + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstallerMetaData metaData = (InstallerMetaData) o;
        return Objects.equals(architecture, metaData.architecture)
            && Objects.equals(location, metaData.location) && Objects.equals(digest, metaData.digest)
            && Objects.equals(dateAdded, metaData.dateAdded)
            && Objects.equals(baseFMWVersion, metaData.baseFMWVersion)
            && Objects.equals(productVersion, metaData.productVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(architecture, location, digest, dateAdded, productVersion, baseFMWVersion);
    }
}
