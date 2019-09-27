// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.WLSCommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddEntryTest {

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
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true, true);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    public void testMissingKey() {
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true, true,
                "--value", "some_value");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required option '--key=<key>'"));
    }

    @Test
    public void testMissingValue() {
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
            true,"--key", "some_key");
        assertTrue(new String(byteArrayOutputStream.toByteArray())
            .contains("Missing required option '--value=<location>'"));
    }

    @Test
    public void testInvalidParameters() {
        CommandResponse response = WLSCommandLine.call(new AddEntry(), true, "--key", "", "--value", "");
        assertEquals(-1, response.getStatus());
    }
}
