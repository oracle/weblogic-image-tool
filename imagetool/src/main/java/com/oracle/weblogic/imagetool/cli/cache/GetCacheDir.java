// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

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

    @Override
    public CommandResponse call() {
        String path = cacheStore.getCacheDir();
        System.out.println("Cache Dir: " + path);
        return new CommandResponse(0, "Cache Dir location: ", path);
    }

}
