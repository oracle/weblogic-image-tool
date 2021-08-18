// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class ImageToolTest {

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
    void testUsage() {
        // No argument should give USAGE
        CommandResponse response = ImageTool.run(new ImageTool(), printStream, printStream);
        assertNotNull(response);
        assertEquals(2, response.getStatus());
        assertTrue(byteArrayOutputStream.toString().contains("Usage: imagetool [OPTIONS]"));
    }

    @Test
    void testHelp() {
        // HELP argument should return usage but success code
        CommandResponse response = ImageTool.run(new ImageTool(), printStream, printStream, "help");
        assertNotNull(response);
        assertEquals(0, response.getStatus());
        assertTrue(byteArrayOutputStream.toString().contains("Usage: imagetool [OPTIONS]"));
    }

    @Test
    void testHelp2() {
        // HELP argument should return usage but success code
        CommandResponse response = ImageTool.run(new ImageTool(), printStream, printStream, "--help");
        assertNotNull(response);
        assertEquals(0, response.getStatus());
        assertTrue(byteArrayOutputStream.toString().contains("Usage: imagetool [OPTIONS]"));
    }
}
