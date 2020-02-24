// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.WLSCommandLine;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AddInstallerEntryTest {

    private ByteArrayOutputStream byteArrayOutputStream = null;
    private PrintStream printStream = null;

    @BeforeEach
    void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(byteArrayOutputStream);
    }

    @AfterEach
    void teardown() {
        if (printStream != null) {
            printStream.close();
        }
    }

    @Test
    void testMissingParameters() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    void testWrongType() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
                "--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Invalid value for option '--type'"));
    }

    @Test
    void testMissingVersion() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
                "--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Missing required option '--version=<version>'"));
    }

    @Test
    void testInvalidParameters() {
        CommandResponse response = WLSCommandLine.call(new AddInstallerEntry(), "--type",
                InstallerType.WLS.toString(), "--version", "", "--path", "/path/to/non/existent/file");
        assertEquals(-1, response.getStatus());
    }
}
