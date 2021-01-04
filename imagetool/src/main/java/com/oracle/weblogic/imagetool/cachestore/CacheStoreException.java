// Copyright (c) 2020. 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cachestore;

import java.io.IOException;

public class CacheStoreException extends IOException {

    public CacheStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
