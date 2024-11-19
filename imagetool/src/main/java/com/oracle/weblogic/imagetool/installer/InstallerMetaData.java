// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Utils;

public class InstallerMetaData {
    private String platform;
    private String location;
    private String digest;
    private String dateAdded;
    private String productVersion;

    /**
     * Constructor InstallerMetaData stores details about this installer.
     * @param platform   platform linux/arm64, linux/amd64
     * @param location   file path of the installer
     * @param digest     sha256 hash value
     * @param dateAdded  date added
     */
    public InstallerMetaData(String platform, String location, String digest, String dateAdded, String productVersion) {
        this.platform = Utils.standardPlatform(platform);
        this.location = location;
        this.digest = digest;
        this.dateAdded = dateAdded;
        this.productVersion = productVersion;
    }

    /**
     * Constructor InstallerMetaData stores details about this installer.
     * @param platform   platform linux/arm64, linux/amd64
     * @param location   file path of the installer
     * @param productVersion real version of this installer
     */
    public InstallerMetaData(String platform, String location, String productVersion) {
        this.platform = Utils.standardPlatform(platform);
        this.location = location;
        this.productVersion = productVersion;
        if (location != null) {
            if (Files.exists(Paths.get(location))) {
                this.digest = Utils.getSha256Hash(location);
                this.dateAdded = Utils.getTodayDate();
            }
        }
    }

    public String getPlatform() {
        return platform;
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

    public String toString() {
        return "InstallerMetaData [platform=" + platform + ", location=" + location + ", hash=" + digest + ", "
            + "dateAdded=" + dateAdded + ", version=" + productVersion + "]";
    }
}
