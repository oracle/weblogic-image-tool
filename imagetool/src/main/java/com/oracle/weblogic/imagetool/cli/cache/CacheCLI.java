// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.List;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Unmatched;

@Command(
        name = "cache",
        description = "List and set cache options",
        versionProvider = HelpVersionProvider.class,
        commandListHeading = "%nCommands:%n%n",
        subcommands = {
                ListCacheItems.class,
                AddInstallerEntry.class,
                AddPatchEntry.class,
                AddEntry.class,
                DeleteEntry.class,
                HelpCommand.class
        },
        sortOptions = false
)
public class CacheCLI implements Callable<CommandResponse> {

    public CacheCLI() {
    }

    @Override
    public CommandResponse call() {
        CommandLine.usage(this, System.out);
        return new CommandResponse(0, "Invalid arguments");
    }

    @Unmatched
    List<String> unmatchedOptions;
}
