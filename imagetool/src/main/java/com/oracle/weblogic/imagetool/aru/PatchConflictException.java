// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.List;

public class PatchConflictException extends AruException {
    public PatchConflictException(List<List<String>> conflictSets) {
        super("The following sets of patches conflict and cannot be applied: " + conflictSets);
    }
}
