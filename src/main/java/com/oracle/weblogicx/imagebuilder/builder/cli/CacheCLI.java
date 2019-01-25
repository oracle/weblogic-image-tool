/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli;

import com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;

@Command(
        name = "cache",
        description = "List and set cache options",
        helpCommand = true,
        mixinStandardHelpOptions = true
)
public class CacheCLI implements Runnable {

    @Option(
            names = { "--listPath" },
            description = "List the cache dir path"
    )
    private boolean listCachePath = false;

    @Option(
            names = { "--listItems" },
            description = "List items in the cache"
    )
    private boolean listCacheItems = false;

    @Option(
            names = { "--setCacheDir" },
            description = "Set cache directory where the tool downloads the artifacts to. Default: $user.home/cache"
    )
    private Path cacheDirPath = null;

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean isCLIMode;

    @Override
    public void run() {
        if (listCachePath) {
            System.out.println("cache dir: " + FileMetaDataResolver.META_RESOLVER.getCacheDir());
        }
        if (listCacheItems) {
            FileMetaDataResolver.META_RESOLVER.getCacheItems().forEach(
                    (key, value) -> System.out.println(key + "=" + value)
            );
        }
        if (cacheDirPath != null) {
            if (!Files.exists(cacheDirPath) || !cacheDirPath.toFile().isDirectory()) {
                cacheDirPath.toFile().mkdirs();
                System.out.println("mkdirs: " + cacheDirPath);
            }
            FileMetaDataResolver.META_RESOLVER.setCacheDir(cacheDirPath.toAbsolutePath().toString());
        }
    }
}
