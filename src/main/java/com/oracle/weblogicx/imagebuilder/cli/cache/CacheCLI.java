/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "cache",
        description = "List and set cache options",
        commandListHeading = "%nCommands:%n%n",
        subcommands = {
                ListCacheItems.class,
                AddInstallerEntry.class,
                AddPatchEntry.class,
                GetCacheDir.class,
                SetCacheDir.class,
                AddEntry.class,
                DeleteEntry.class,
                HelpCommand.class
        },
        sortOptions = false
)
public class CacheCLI implements Callable<CommandResponse> {

    public CacheCLI() {
    }

    public CacheCLI(boolean isCLIMode) {
        this.isCLIMode = isCLIMode;
    }

    @Override
    public CommandResponse call() {
        if (isCLIMode) {
            spec.commandLine().usage(System.out);
        }
        return new CommandResponse(-1, "Invalid arguments");
    }

    private boolean isCLIMode;

    @Spec
    CommandSpec spec;

    @Unmatched
    List<String> unmatchedOptions;
}
