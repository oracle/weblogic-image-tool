// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.util.logging.Level;

import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class CommonPatchingOptions2Test {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);
    }

    @AfterAll
    static void tearDown() throws NoSuchFieldException, IllegalAccessException {
        logger.setLevel(oldLevel);
    }

    @Test
    void noPassword() {
        // This test requires that ARUUtil instance NOT be overridden
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "tag:1", "--user", "derek");
        assertThrows(InvalidCredentialException.class, createImage::initializeOptions);
    }

}
