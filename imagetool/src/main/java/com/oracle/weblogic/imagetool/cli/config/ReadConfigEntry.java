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
    name = "show",
    description = "Print a configuration entry",
    sortOptions = false
)
public class ReadConfigEntry  implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(ReadConfigEntry.class);

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

        String result = name.get(settings);

        logger.exiting();
        if (result != null) {
            System.out.println(result);
            return new CommandResponse(0, "");
        } else {
            return new CommandResponse(CommandLine.ExitCode.SOFTWARE, "Not Found");
        }
    }
}
