// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.cli.WLSCommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddInstallerEntryTest {

    private ByteArrayOutputStream byteArrayOutputStream = null;
    private PrintStream printStream = null;

    @Before
    public void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(byteArrayOutputStream);
    }

    @After
    public void teardown() {
        if (printStream != null) {
            printStream.close();
        }
    }

    @Test
    public void testMissingParameters() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    public void testWrongType() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
                "--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Invalid value for option '--type'"));
    }

    @Test
    public void testMissingVersion() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
                "--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Missing required option '--version=<version>'"));
    }

    @Test
    public void testInvalidParameters() {
        CommandResponse response = WLSCommandLine.call(new AddInstallerEntry(), "--type",
                InstallerType.WLS.toString(), "--version", "", "--path", "/path/to/non/existent/file");
        assertEquals(-1, response.getStatus());
    }
}
