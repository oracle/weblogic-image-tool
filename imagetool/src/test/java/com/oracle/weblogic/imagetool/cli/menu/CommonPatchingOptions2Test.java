// Copyright (c) 2022, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.test.annotations.ReduceTestLogging;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
@ReduceTestLogging(loggerClass = AruUtil.class)
class CommonPatchingOptions2Test {

    @Test
    void noPassword() {
        // This test requires that ARUUtil instance NOT be overridden
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek", "--patches", "12345678");
        assertThrows(InvalidCredentialException.class, createImage::initializeOptions);
    }

}
