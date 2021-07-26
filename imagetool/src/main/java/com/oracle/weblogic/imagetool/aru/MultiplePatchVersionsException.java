// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.util.Utils;

public class MultiplePatchVersionsException extends IOException {

    /**
     * Signals that the bug number provided was not unique, and has multiple versions available.
     *
     * @param bugNumber         the bug number that was searched
     * @param versionsAvailable the list of versions for patches of that bug
     */
    public MultiplePatchVersionsException(String bugNumber, List<AruPatch> versionsAvailable) {
        super(Utils.getMessage("IMG-0034", bugNumber,
            versionsAvailable.stream()
                .map(s -> bugNumber + "_" + s.version())
                .collect(Collectors.joining(", ")))
        );
    }
}
