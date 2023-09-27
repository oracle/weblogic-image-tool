// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
import picocli.CommandLine.Command;

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
        DeleteEntry.class
    },
    sortOptions = false
)
public class CacheCLI {
}
