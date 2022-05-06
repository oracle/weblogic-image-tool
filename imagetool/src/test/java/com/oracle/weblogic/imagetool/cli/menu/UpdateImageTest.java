// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class UpdateImageTest {
    @Test
    void installerTypeTest() {
        UpdateImage updateImage = new UpdateImage();
        new CommandLine(updateImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "abc");
        // when not discovered from the fromImage, default should be WLS
        assertEquals(FmwInstallerType.WLS, updateImage.getInstallerType());

        updateImage.setImageInstallerType("WLS,COH,TOPLINK,JDBC,FIT,INFRA,OPSS,OWSM"); // mimic inspect FMW image
        // when discovered from the fromImage, value should be as discovered
        assertEquals(FmwInstallerType.FMW, updateImage.getInstallerType());
    }

    @Test
    void installerTypeOverrideTest() {
        UpdateImage updateImage = new UpdateImage();
        new CommandLine(updateImage).parseArgs("--tag", "tag:1", "--user", "derek", "--password", "xxx",
            "--patches", "abc", "--type", "SOA");
        // when not discovered from the fromImage, value should be same as provided by user (not default)
        assertEquals(FmwInstallerType.SOA, updateImage.getInstallerType());

        updateImage.setImageInstallerType("FMW");
        // when discovered from the fromImage, value should be same as provided by user
        assertEquals(FmwInstallerType.SOA, updateImage.getInstallerType());
    }
}

