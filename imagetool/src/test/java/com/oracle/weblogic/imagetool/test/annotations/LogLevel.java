// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.test.annotations;

import java.util.logging.Level;

/**
 * An Enum representation of the Logger levels to be used with the TestLoggerOverride Annotation.
 */
public enum LogLevel {
    INFO(Level.INFO),
    WARNING(Level.WARNING),
    SEVERE(Level.SEVERE),
    OFF(Level.OFF);

    private final Level value;

    LogLevel(Level value) {
        this.value = value;
    }

    public Level value() {
        return value;
    }
}
