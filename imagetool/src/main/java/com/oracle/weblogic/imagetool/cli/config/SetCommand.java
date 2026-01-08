// Copyright (c) 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "set",
    description = "Set or update a configuration entry",
    sortOptions = false
)
public class SetCommand implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(SetCommand.class);

    @Option(
        names = {"--name"},
        description = "Name of the setting",
        order = 0,
        required = true
    )
    private ConfigAttributeName name;

    @Parameters(index = "0", description = "the new configuration setting value")
    @SuppressWarnings("UnusedDeclaration")
    private String value;

    @Override
    public CommandResponse call() throws Exception {
        UserSettingsFile settings = new UserSettingsFile();
        logger.entering();
        int status = 0;
        String message = "done";
        if (verifyInput(name.toString(), value)) {
            name.set(settings, value);
            settings.save();
        } else {
            status = -1;
            message = "Failed to set configuration entry";
        }

        logger.exiting();
        return new CommandResponse(status, message);
    }

    private boolean verifyInput(String name, String value) {
        if ("aruRetryInterval".equalsIgnoreCase(name) || "aruRetryMax".equalsIgnoreCase(name)) {
            try {
                int n = Integer.parseInt(value);
                if (n < 0) {
                    logger.severe("IMG-0161", name, value);
                    return false;
                }
            } catch (NumberFormatException e) {
                logger.severe("IMG-0161", name, value);
                return false;
            }
        }

        if ("defaultBuildPlatform".equalsIgnoreCase(name) || "containerEngine".equalsIgnoreCase(name)) {
            if (!"linux/amd64".equals(value) && !"linux/arm64".equals(value)) {
                logger.severe("IMG-0162");
                return false;
            }
        }

        if ("buildContextDirectory".equalsIgnoreCase(name) || "patchDirectory".equalsIgnoreCase(name)
            || "installerDirectory".equalsIgnoreCase(name)) {
            if (!Files.isDirectory(Paths.get(value))) {
                logger.severe("IMG-0163", name, value);
                return false;
            }
            return true;
        }
        return true;
    }
}
