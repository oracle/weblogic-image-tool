/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(
        name = "setCacheDir",
        description = "Sets the cache directory where to download required artifacts"
)
public class SetCacheDir extends CacheOperation {

    public SetCacheDir() {
    }

    public SetCacheDir(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {

        if (location != null && !Files.isDirectory(location)) {
            return new CommandResponse(-1, "cache dir should be an existing directory on local disk");
        }

        String oldValue = cacheStore.getCacheDir();
        String msg;
        if (oldValue != null) {
            msg = String.format("Changed cache dir from %s to %s", oldValue, location);
        } else {
            msg = String.format("Set cache dir to: %s", location);
        }

        if (cacheStore.setCacheDir(location.toAbsolutePath().toString())) {
            if (isCLIMode) {
                System.out.println(msg);
            }
            return new CommandResponse(0, msg);
        } else {
            return new CommandResponse(-1, "setCacheDir failed");
        }
    }

    @Parameters(
            arity = "1",
            description = "A directory on local disk. Ex: /path/to/my/dir",
            index = "0"
    )
    private Path location;
}
