// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.ImageTool;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AddInstallerEntryTest {

    private ByteArrayOutputStream byteArrayOutputStream = null;
    private PrintWriter printStream = null;

    @BeforeEach
    void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        printStream = new PrintWriter(byteArrayOutputStream);
    }

    @AfterEach
    void teardown() {
        if (printStream != null) {
            printStream.close();
        }
    }

    @Test
    void testMissingParameters() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    void testWrongType() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream,
                "--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Invalid value for option '--type'"));
    }

    @Test
    void testMissingVersion() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream,
                "--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Missing required option: '--version=<version>'"));
    }

    @Test
    void testInvalidParameters() {
        CommandResponse response = ImageTool.run(new AddInstallerEntry(), printStream, printStream, "--type",
                InstallerType.WLS.toString(), "--version", "", "--path", "/path/to/non/existent/file");
        assertEquals(-1, response.getStatus());
    }
}
