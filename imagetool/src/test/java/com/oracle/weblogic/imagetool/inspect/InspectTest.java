// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
class InspectTest {
    @Test
    void testJsonOutput() throws IOException {
        testPropertiesToJson("src/test/resources/inspect/image1.properties",
            "src/test/resources/inspect/image1.json");
    }

    @Test
    void testEmptyPatches() throws IOException {
        testPropertiesToJson("src/test/resources/inspect/image2.properties",
            "src/test/resources/inspect/image2.json");
    }

    void testPropertiesToJson(String propsFile, String jsonFile) throws IOException {
        Properties loaded = new Properties();
        try (InputStream input = new FileInputStream(propsFile)) {
            loaded.load(input);
        }

        FileReader expected = new FileReader(jsonFile);
        StringReader actual = new StringReader(new InspectOutput(loaded).toString());
        assertReaders(new BufferedReader(expected), new BufferedReader(actual));
    }

    private static void assertReaders(BufferedReader expected, BufferedReader actual) throws IOException {
        String line;
        while ((line = expected.readLine()) != null) {
            assertEquals(line, actual.readLine());
        }

        assertNull(actual.readLine(), "Output had more lines then the expected.");
        assertNull(expected.readLine(), "Output had fewer lines then the expected.");
    }
}
