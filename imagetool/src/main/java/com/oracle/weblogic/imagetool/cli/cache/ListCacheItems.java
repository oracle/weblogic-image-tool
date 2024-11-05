// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(
        name = "listItems",
        description = "List cache contents"
)
public class ListCacheItems extends CacheOperation {

    @Override
    public CommandResponse call() throws CacheStoreException {
        String fileName;
        if ("patches".equalsIgnoreCase(type)) {
            fileName = ConfigManager.getInstance().getPatchDetailsFile();
        } else {
            fileName = ConfigManager.getInstance().getInstallerDetailsFile();
        }
        if (fileName != null) {
            try {
                Path path = Paths.get(fileName);
                Files.lines(path).forEach(System.out::println);
            } catch (IOException ioException) {
                System.err.println("Unable to read file: " + fileName);
            }
        }
        return CommandResponse.success(null);

    }

    @Option(
        names = {"--type"},
        description = "list type type : patches, installers (default: installers)"
    )
    private String type;

}
