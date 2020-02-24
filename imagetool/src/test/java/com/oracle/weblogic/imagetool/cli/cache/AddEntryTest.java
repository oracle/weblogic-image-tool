// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.ImageTool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddEntryTest {

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
        ImageTool.run(new AddEntry(), printStream, printStream);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    public void testMissingKey() {
        ImageTool.run(new AddEntry(), printStream, printStream, "--value", "some_value");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required option '--key=<key>'"));
    }

    @Test
    public void testMissingValue() {
        ImageTool.run(new AddEntry(), printStream, printStream, "--key", "some_key");
        assertTrue(new String(byteArrayOutputStream.toByteArray())
            .contains("Missing required option '--value=<location>'"));
    }

    @Test
    public void testInvalidParameters() {
        CommandResponse response = ImageTool.run(new AddEntry(), printStream, printStream, "--key", "", "--value", "");
        assertEquals(-1, response.getStatus());
    }
}
