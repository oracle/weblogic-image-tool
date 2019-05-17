package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.cli.WLSCommandLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

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
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true, true);
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains("Missing required options"));
    }

    @Test
    public void testWrongType() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true, true,
                "--type", "a2z", "--version", "some_value", "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Invalid value for option '--type'"));
    }

    @Test
    public void testMissingVersion() {
        WLSCommandLine.call(new AddInstallerEntry(), printStream, printStream, CommandLine.Help.Ansi.AUTO, true, true,
                "--type", InstallerType.WLS.toString(), "--path", "/path/to/a/file");
        assertTrue(new String(byteArrayOutputStream.toByteArray()).contains(
                "Missing required option '--version=<version>'"));
    }

    @Test
    public void testInvalidParameters() {
        CommandResponse response = WLSCommandLine.call(new AddInstallerEntry(), true, "--type",
                InstallerType.WLS.toString(), "--version", "", "--path", "/path/to/non/existent/file");
        assertEquals(-1, response.getStatus());
    }
}
