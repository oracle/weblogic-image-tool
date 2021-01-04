// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.util.List;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.util.Utils;

/**
 * For a given list of patches returned from ARU, the version requested was not found.
 */
public class VersionNotFoundException extends AruException {
    /**
     * For a given list of patches returned from ARU, the version requested was not found.
     * @param patchId patch ID that was used to search
     * @param version the version that was requested
     * @param patches the patches that ARU returned
     */
    public VersionNotFoundException(String patchId, String version, List<AruPatch> patches) {
        super(Utils.getMessage("IMG-0083", version, patchId,
            patches.stream()
            .map(s -> s.patchId() + "_" + s.version())
            .collect(Collectors.joining(", "))));
    }
}
