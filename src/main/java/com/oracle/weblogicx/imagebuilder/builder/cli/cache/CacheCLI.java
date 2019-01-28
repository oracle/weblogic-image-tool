/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
        name = "cache",
        description = "List and set cache options",
        commandListHeading = "%nCommands:%n%n",
        //mixinStandardHelpOptions = true,
        subcommands = {
                ListCacheItems.class,
                AddInstallerEntry.class,
                AddPatchEntry.class,
                GetCacheDir.class,
                SetCacheDir.class,
                AddEntry.class,
                DeleteEntry.class
        },
        sortOptions = false
)
public class CacheCLI implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() {
        if (cliMode) {
            CommandLine.usage(new CacheCLI(), System.out);
        }
        return new CommandResponse(-1, "Invalid arguments");
    }

    @Option(
            names = {"--cli"},
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;
}
