// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;

import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "set",
    description = "Set or update a configuration entry",
    sortOptions = false
)
public class SetConfigEntry implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(SetConfigEntry.class);

    @Option(
        names = {"--name"},
        description = "Name of the setting",
        order = 0,
        required = true
    )
    private ConfigAttributeName name;

    @Parameters(index = "0")
    @SuppressWarnings("UnusedDeclaration")
    private String value;

    @Override
    public CommandResponse call() throws Exception {
        UserSettings settings = UserSettings.load();
        logger.entering();

        name.set(settings, value);
        settings.save();

        logger.exiting();
        return new CommandResponse(0, "done");
    }
}
