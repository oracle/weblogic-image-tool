// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Convert image properties to JSON.
 * This class should be replaced if/when a full JSON parser is added to the project.
 */
public class InspectOutput {
    private static final String PATCHES_KEY = "oraclePatches";
    Map<String,String> attributes;
    List<InventoryPatch> patches;
    OperatingSystemProperties os;

    /**
     * Convert image properties to JSON output.
     * @param imageProperties Properties from the image.
     */
    public InspectOutput(Properties imageProperties) {
        // convert Properties to TreeMap (to sort attributes alphabetically)
        Map<String,String> sorted = imageProperties.entrySet().stream()
            .map(InspectOutput::convertToStringEntry)
            .filter(e -> !e.getKey().equals(PATCHES_KEY)) // do not store patches entry as a normal attribute
            .filter(e -> !e.getKey().startsWith("__OS__")) // do not store OS entries as a normal attribute
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (v1, v2) -> v1, // discard duplicates, but there shouldn't be any dupes
                TreeMap::new)); // use a sorted map

        if (imageProperties.containsKey(PATCHES_KEY)) {
            patches = InventoryPatch.parseInventoryPatches(imageProperties.get(PATCHES_KEY).toString());
        }

        attributes = sorted;
        os = OperatingSystemProperties.getOperatingSystemProperties(imageProperties);
    }

    private static Map.Entry<String,String> convertToStringEntry(Map.Entry<Object,Object> entry) {
        return new AbstractMap.SimpleEntry<>(entry.getKey().toString(), entry.getValue().toString());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("{\n");
        if (patches != null) {
            result.append(pad(1)).append('\"').append(PATCHES_KEY).append('\"').append(" : [\n");
            Iterator<InventoryPatch> patchesIter = patches.iterator();
            while (patchesIter.hasNext()) {
                InventoryPatch patch = patchesIter.next();
                result.append(pad(2)).append('{').append('\n');
                result.append(jsonKeyValuePair(3, "patch", patch.bug())).append(",\n");
                result.append(jsonKeyValuePair(3, "description", patch.description())).append('\n');
                result.append(pad(2)).append('}');
                if (patchesIter.hasNext()) {
                    result.append(',');
                }
                result.append('\n');
            }
            result.append(pad(1)).append("],\n");
        }
        if (os != null) {
            result.append(pad(1)).append('\"').append("os").append('\"').append(" : {\n");
            result.append(jsonKeyValuePair(2, "id", os.id())).append(",\n");
            result.append(jsonKeyValuePair(2, "name", os.name())).append(",\n");
            result.append(jsonKeyValuePair(2, "version", os.version())).append("\n");
            result.append(pad(1)).append("},");
            result.append('\n');
        }
        Iterator<Map.Entry<String,String>> attributeIter = attributes.entrySet().iterator();
        while (attributeIter.hasNext()) {
            Map.Entry<String,String> entry = attributeIter.next();
            result.append(jsonKeyValuePair(1, entry.getKey(), entry.getValue()));
            if (attributeIter.hasNext()) {
                result.append(',');
            }
            result.append('\n');
        }
        result.append(pad(0)).append('}');
        return result.toString();
    }

    private String jsonKeyValuePair(int indent, String key, String value) {
        return new String(pad(indent)) + String.format("\"%s\" : \"%s\"", key, value);
    }

    private char[] pad(int size) {
        char[] result = new char[size * 2];
        Arrays.fill(result, ' ');
        return result;
    }
}
