// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AdditionalBuildCommandsTest {

    private static Path getPath(String filename) {
        return Paths.get("src/test/resources/additionalBuildCommands/" + filename);
    }

    @Test
    public void loadSingleSectionFile() throws IOException {
        String filename = "one-section.txt";
        AdditionalBuildCommands cmds = AdditionalBuildCommands.load(getPath(filename));

        assertEquals("File did not have expected number of sections: " + filename, 1, cmds.size());
    }

    @Test
    public void loadTwoSectionFile() throws IOException {
        String filename = "two-sections.txt";
        AdditionalBuildCommands cmds = AdditionalBuildCommands.load(getPath(filename));
        assertEquals("File did not have expected number of sections: " + filename, 2, cmds.size());
        assertEquals("Wrong number of lines in AFTER_FMW section: " + filename,
            3, cmds.getSection(AdditionalBuildCommands.AFTER_FMW).size());
        assertEquals("Wrong number of lines in BEFORE_WDT section: " + filename,
            3, cmds.getSection(AdditionalBuildCommands.BEFORE_JDK).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadBadSectionFile() throws IOException {
        String filename = "bad-section.txt";
        AdditionalBuildCommands cmds = AdditionalBuildCommands.load(getPath(filename));
        //should never reach this line, load() should throw an exception
        fail();
    }
}