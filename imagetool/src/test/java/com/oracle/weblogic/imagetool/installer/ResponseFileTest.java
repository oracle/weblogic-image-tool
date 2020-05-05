// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
public class ResponseFileTest {
    @Test
    void copyProvidedResponseFile(@TempDir File tmpDir) throws Exception {
        String filename = "responseA.txt";
        Path sourceFile = Paths.get("./src/test/resources/responseFiles/" + filename);

        ResponseFile responseFile0 = new ProvidedResponseFile(sourceFile);
        assertEquals(filename, responseFile0.name(), "failed to load installer response file name");

        responseFile0.copyFile(tmpDir.getAbsolutePath());
        assertEquals(filename, responseFile0.name(), "file name should not have changed after copy");

        ResponseFile responseFile1 = new ProvidedResponseFile(sourceFile);
        responseFile1.copyFile(tmpDir.getAbsolutePath());
        assertEquals(filename + "1", responseFile1.name(), "file name should have changed after copy");

        ResponseFile responseFile2 = new ProvidedResponseFile(sourceFile);
        responseFile2.copyFile(tmpDir.getAbsolutePath());
        assertEquals(filename + "2", responseFile2.name(), "file name should have changed after copy");
    }

    @Test
    void notRegularFile(@TempDir File tmpDir) throws Exception {
        // not a regular file, should skip copy request
        Path directory = Paths.get("./src/test/resources/responseFiles");
        ResponseFile responseFile = new ProvidedResponseFile(directory);
        responseFile.copyFile(tmpDir.getAbsolutePath());
        assertNull(responseFile.name(), "directories are not valid for a response file");
    }
}
