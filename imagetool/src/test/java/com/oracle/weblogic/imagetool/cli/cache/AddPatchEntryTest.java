// Copyright (c) 2020. 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
public class AddPatchEntryTest {
    private static LoggingFacade commandLogger = LoggingFactory.getLogger(AddPatchEntry.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() throws NoSuchFieldException, IllegalAccessException {
        // disable logging for the tested tool to prevent filling up the screen with errors (that are expected)
        oldLevel = commandLogger.getLevel();
        commandLogger.setLevel(Level.OFF);
    }

    @AfterAll
    static void tearDown() {
        commandLogger.setLevel(oldLevel);
    }

    private static CommandLine getCommand() {
        AddPatchEntry app = new AddPatchEntry();
        CommandLine cmd = new CommandLine(app);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        cmd.setErr(new PrintWriter(sw));
        return cmd;
    }

    @Test
    void testMissingParameters() {
        CommandLine cmd = getCommand();
        // missing valid parameters should generate USAGE output
        int exitCode = cmd.execute("-x", "-y=123");
        CommandResponse result = cmd.getExecutionResult();
        assertNull(result, "Should not have a response, picoli should intercept usage error");
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    void testInvalidFileParameter() {
        CommandLine cmd = getCommand();
        // invalid file (file does not exist), should generate an error response
        int exitCode = cmd.execute("--patchId=12345678_12.2.1.3.0", "--path=/here/there");
        CommandResponse result = cmd.getExecutionResult();
        assertNotNull(result, "Response missing from call to addPatch");
        assertEquals(-1, result.getStatus());
    }

    @Test
    void testInvalidPatchId() {
        CommandLine cmd = getCommand();
        // invalid patch ID should generate an error response
        cmd.execute("--patchId=12345678", "--path=pom.xml");
        CommandResponse result = cmd.getExecutionResult();
        assertNotNull(result, "Response missing from call to addPatch");
        assertEquals(-1, result.getStatus());
    }
}
