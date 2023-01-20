// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;

import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettings;
import picocli.CommandLine;

@CommandLine.Command(
    name = "reset",
    description = "Remove/reset a configuration entry",
    sortOptions = false
)
public class ResetCommand implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(ResetCommand.class);

    @CommandLine.Option(
        names = {"--name"},
        description = "Name of the setting",
        order = 0,
        required = true
    )
    private ConfigAttributeName name;

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        UserSettings settings = UserSettings.load();

        name.set(settings, null);
        settings.save();

        logger.exiting();
        return new CommandResponse(0, "");
    }
}
