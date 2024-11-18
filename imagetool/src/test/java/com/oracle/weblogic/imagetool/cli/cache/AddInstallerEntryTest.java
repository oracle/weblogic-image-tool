// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.installer.InstallerType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AddInstallerEntryTest {
    @Test
    void testMissingParameters() {
        final AddInstallerEntry addCommand = new AddInstallerEntry();
        // Empty command line throws exception because the required parameters were not specified
        assertThrows(CommandLine.MissingParameterException.class, () ->
            new CommandLine(addCommand).parseArgs()
        );
    }

    @Test
    void testWrongType() {
        final AddInstallerEntry addCommand = new AddInstallerEntry();
        // The value for --type must be one of the pre-defined types
        CommandLine.ParameterException pe = assertThrows(CommandLine.ParameterException.class, () ->
            new CommandLine(addCommand)
                .parseArgs("--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file")
        );
        assertTrue(pe.getMessage().contains("--type"));

        // repeat same command but use a valid type.  No exception should be thrown.
        new CommandLine(addCommand)
            .parseArgs("--type", "WLS", "--version", "some_value", "--path", "/path/to/a/file");
    }

    @Test
    void testMissingVersion() {
        final AddInstallerEntry addCommand = new AddInstallerEntry();
        // The value for --version must be specified
        CommandLine.ParameterException pe = assertThrows(CommandLine.ParameterException.class, () ->
            new CommandLine(addCommand)
                .parseArgs("--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file")
        );
        assertTrue(pe.getMessage().contains("--version"));
    }

    @Test
    void testValidParameters() {
        final AddInstallerEntry addCommand = new AddInstallerEntry();
        // The cache key should be a string made up of the type and version seperated by an underscore
        new CommandLine(addCommand)
            .parseArgs("--type", "WLS", "--version", "12.2.1.4", "--path", "/path/to/a/file");
        assertEquals("wls", addCommand.getKey());
    }

    @Test
    void testArchKey() {
        final AddInstallerEntry addCommand = new AddInstallerEntry();
        // The cache key should be a string made up of the type, version, and architecture seperated by an underscore
        new CommandLine(addCommand)
            .parseArgs("--type", "WLS", "--version", "12.2.1.4", "-a", "amd64", "--path", "/path/to/a/file");
        assertEquals("wls", addCommand.getKey());
    }
}
