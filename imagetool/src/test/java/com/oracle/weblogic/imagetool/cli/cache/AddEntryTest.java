// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.WLSCommandLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AddEntryTest {

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
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    void testMissingKey() {
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
                "--value", "some_value");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required option '--key=<key>'"));
    }

    @Test
    void testMissingValue() {
        WLSCommandLine.call(new AddEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true,
            "--key", "some_key");
        assertTrue(new String(byteArrayOutputStream.toByteArray())
            .contains("Missing required option '--value=<location>'"));
    }

    @Test
    void testInvalidParameters() {
        CommandResponse response = WLSCommandLine.call(new AddEntry(), "--key", "", "--value", "");
        assertEquals(-1, response.getStatus());
    }
}
