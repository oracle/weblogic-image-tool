// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.List;

import com.oracle.weblogic.imagetool.util.Utils;

public class PatchConflictException extends AruException {
    public PatchConflictException(List<List<String>> conflictSets) {
        super(createMessage(conflictSets));
    }

    private static String createMessage(List<List<String>> conflictSets) {
        StringBuilder result = new StringBuilder();
        result.append(Utils.getMessage("IMG-0116"));
        result.append(" ");
        int numberOfSets = conflictSets.size();
        for (int i = 0; i < numberOfSets; i++) {
            result.append(conflictSets.get(i));
            if (i + 1  < numberOfSets) {
                result.append(", ");
            }
        }
        return result.toString();
    }
}
