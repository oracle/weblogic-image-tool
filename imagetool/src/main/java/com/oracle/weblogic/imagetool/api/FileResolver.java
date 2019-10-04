// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;

public interface FileResolver {

    /**
     * Given a cache store, check whether the installer / patch file exists on disk.
     * files will be downloaded if cachePolicy allows it.
     *
     * @param cacheStore store that keeps track of required artifacts
     * @return location of file on disk if found
     * @throws Exception in case of error
     */
    String resolve(CacheStore cacheStore) throws Exception;

}
