// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        AdditionalBuildCommands cmds = new AdditionalBuildCommands(getPath(filename));

        assertEquals(1, cmds.size(), "File did not have expected number of sections: " + filename);
    }

    @Test
    void loadTwoSectionFile() throws IOException {
        String filename = "two-sections.txt";
        AdditionalBuildCommands cmds = new AdditionalBuildCommands(getPath(filename));
        assertEquals(2, cmds.size(), "File did not have expected number of sections: " + filename);
        assertEquals(3, cmds.getSection(AdditionalBuildCommands.AFTER_FMW).size(),
            "Wrong number of lines in AFTER_FMW section of " + filename);
        assertEquals(2, cmds.getSection(AdditionalBuildCommands.BEFORE_JDK).size(),
            "Wrong number of lines in BEFORE_JDK section of " + filename);
    }

    @Test
    void loadBadSectionFile() {
        Path file = getPath("bad-section.txt");
        assertThrows(IllegalArgumentException.class, () -> new AdditionalBuildCommands(file));
    }

    @Test
    void resolveMustachePlaceHolders() throws Exception {
        AdditionalBuildCommands cmds = new AdditionalBuildCommands(getPath("two-mustache.txt"));
        DockerfileOptions options = new DockerfileOptions("123");
        // default Oracle Home and Java Home are expected, /u01/oracle, /u01/jdk.
        options.setAdditionalBuildCommands(cmds.getContents(options));

        List<String> expected = Arrays.asList(
            "echo This is the Oracle Home: /u01/oracle",
            "echo This is the Java Home: /u01/jdk");
        assertEquals(expected, options.finalBuildCommands());

        // If a section is requested that is NOT in the file, the getter should return an empty list.
        assertEquals(Collections.emptyList(), options.beforeFmwInstall());
    }
}