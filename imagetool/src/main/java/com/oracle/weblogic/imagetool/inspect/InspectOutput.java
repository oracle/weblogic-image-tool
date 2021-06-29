// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.inspect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Convert image properties to JSON.
 * This class should be replaced if/when a full JSON parser is added to the project.
 */
public class InspectOutput {
    private final String patchesKey = "oraclePatches";
    Map<String,String> attributes;
    List<PatchJson> patches;

    /**
     * Convert image properties to JSON output.
     * @param imageProperties Properties from the image.
     */
    public InspectOutput(Properties imageProperties) {
        Map<String,String> sortedSet = new TreeMap<>();
        for (Map.Entry<Object,Object> x: imageProperties.entrySet()) {
            sortedSet.put(x.getKey().toString(), x.getValue().toString());
        }
        if (sortedSet.containsKey(patchesKey)) {
            patches = new ArrayList<>();
            String patchesValue = sortedSet.get(patchesKey);
            if (!Utils.isEmptyString(patchesValue)) {
                String[] tokens = patchesValue.split(";");
                for (int i = 0; i < tokens.length; i++) {
                    PatchJson patch = new PatchJson();
                    patch.bug = tokens[i];
                    if (i++ < tokens.length) {
                        patch.uid = tokens[i];
                    }
                    if (i++ < tokens.length) {
                        patch.description = tokens[i].replace("\"", "");
                    }
                    patches.add(patch);
                }
            }
            sortedSet.remove(patchesKey);
        }
        attributes = sortedSet;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append("{\n");
        if (patches != null) {
            result.append(pad(1)).append('\"').append(patchesKey).append('\"').append(" : [\n");
            Iterator<PatchJson> patchesIter = patches.iterator();
            while (patchesIter.hasNext()) {
                PatchJson patch = patchesIter.next();
                result.append(pad(2)).append('{').append('\n');
                result.append(jsonKeyValuePair(3, "patch", patch.bug)).append(",\n");
                result.append(jsonKeyValuePair(3, "description", patch.description)).append('\n');
                result.append(pad(2)).append('}');
                if (patchesIter.hasNext()) {
                    result.append(',');
                }
                result.append('\n');
            }
            result.append(pad(1)).append("],\n");
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

    private static class PatchJson {
        public String bug;
        public String uid;
        public String description;
    }
}
