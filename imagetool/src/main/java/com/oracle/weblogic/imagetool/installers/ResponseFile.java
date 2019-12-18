// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.installers;

import java.io.IOException;

public interface ResponseFile {
    void copyFile(String buildContextDir) throws IOException;

    String name();
}
