/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
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
