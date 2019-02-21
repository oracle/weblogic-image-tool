/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(
        name = "setCacheDir",
        description = "Sets the cache directory where to download required artifacts"
)
public class SetCacheDir extends CacheOperation {

    @Override
    public CommandResponse call() {
        String oldValue = cacheStore.getCacheDir();
        String msg;
        if (oldValue != null) {
            msg = String.format("Changed cache dir from %s to %s", oldValue, location);
        } else {
            msg = String.format("Set cache dir to: %s", location);
        }

        if (cacheStore.setCacheDir(location.toAbsolutePath().toString())) {
            if (unmatchedOptions.contains(Constants.CLI_OPTION)) {
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

//    @Option(
//            names = {"--cacheStoreType"},
//            description = "Whether to use file backed cache store or preferences backed cache store. Ex: file or pref",
//            hidden = true,
//            defaultValue = "file"
//    )
//    private CacheStore cacheStore;
//
//    @Unmatched
//    List<String> unmatchedOptions;
}
