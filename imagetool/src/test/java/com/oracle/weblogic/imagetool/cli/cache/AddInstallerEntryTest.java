// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.ImageTool;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddInstallerEntryTest {

    private ByteArrayOutputStream byteArrayOutputStream = null;
    private PrintWriter printStream = null;

    @Before
    public void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        printStream = new PrintWriter(byteArrayOutputStream);
    }

    @After
    public void teardown() {
        if (printStream != null) {
            printStream.close();
        }
    }

    @Test
    public void testMissingParameters() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    public void testWrongType() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream,
                "--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Invalid value for option '--type'"));
    }

    @Test
    public void testMissingVersion() {
        ImageTool.run(new AddInstallerEntry(), printStream, printStream,
                "--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Missing required option '--version=<version>'"));
    }

    @Test
    public void testInvalidParameters() {
        CommandResponse response = ImageTool.run(new AddInstallerEntry(), printStream, printStream, "--type",
                InstallerType.WLS.toString(), "--version", "", "--path", "/path/to/non/existent/file");
        assertEquals(-1, response.getStatus());
    }
}
