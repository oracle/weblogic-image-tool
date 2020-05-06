// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class AdditionalBuildCommandsTest {

    private static Path getPath(String filename) {
        return Paths.get("src/test/resources/additionalBuildCommands/" + filename);
    }

    @Test
    void loadSingleSectionFile() throws IOException {
        String filename = "one-section.txt";
        AdditionalBuildCommands cmds = AdditionalBuildCommands.load(getPath(filename));

        assertEquals(1, cmds.size(), "File did not have expected number of sections: " + filename);
    }

    @Test
    void loadTwoSectionFile() throws IOException {
        String filename = "two-sections.txt";
        AdditionalBuildCommands cmds = AdditionalBuildCommands.load(getPath(filename));
        assertEquals(2, cmds.size(), "File did not have expected number of sections: " + filename);
        assertEquals(3, cmds.getSection(AdditionalBuildCommands.AFTER_FMW).size(),
            "Wrong number of lines in AFTER_FMW section of " + filename);
        assertEquals(2, cmds.getSection(AdditionalBuildCommands.BEFORE_JDK).size(),
            "Wrong number of lines in BEFORE_JDK section of " + filename);
    }

    @Test
    void loadBadSectionFile() throws IOException {
        String filename = "bad-section.txt";
        assertThrows(IllegalArgumentException.class, () -> {
            AdditionalBuildCommands.load(getPath(filename));
        });
    }
}