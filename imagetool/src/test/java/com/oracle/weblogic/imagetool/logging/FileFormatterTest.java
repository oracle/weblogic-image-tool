// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


class FileFormatterTest {

    @Test
    void format() {
        FileFormatter formatter = new FileFormatter();
        LogRecord logRecord = new LogRecord(Level.INFO, "message goes here");

        String logMessage = formatter.format(logRecord);
        assertTrue(logMessage.startsWith("####"));
        assertTrue(logMessage.endsWith("<message goes here>\n"));
    }

    @Test
    void formatWithColor() {
        FileFormatter formatter = new FileFormatter();
        String initialValue = "message [[brightred: goes]] here";
        LogRecord logRecord = new LogRecord(Level.INFO, initialValue);

        String logMessage = formatter.format(logRecord);
        // color should be removed for non-console logging
        assertTrue(logMessage.endsWith("<message goes here>\n"));
    }
}