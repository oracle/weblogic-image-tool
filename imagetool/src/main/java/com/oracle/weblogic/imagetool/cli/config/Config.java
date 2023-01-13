// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;

import picocli.CommandLine;

@CommandLine.Command(
    name = "config",
    description = "Set global configuration options and defaults for the Image Tool",
    commandListHeading = "%nCommands:%n%n",
    subcommands = {
        SetConfigEntry.class,
        ReadConfigEntry.class,
        DeleteConfigEntry.class
    },
    sortOptions = false
)
public class Config {
}
