// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import picocli.CommandLine.Command;

@Command(
        name = "getCacheDir",
        description = "Prints the cache directory path"
)
public class GetCacheDir extends CacheOperation {

    public GetCacheDir() {
    }

    public GetCacheDir(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {
        String path = cacheStore.getCacheDir();
        if (isCLIMode) {
            System.out.println("Cache Dir: " + path);
        }
        return new CommandResponse(0, "Cache Dir location: ", path);
    }

}
