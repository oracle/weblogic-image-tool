// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.config;

import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.UserSettingsFile;
import picocli.CommandLine;

@CommandLine.Command(
    name = "show",
    description = "Print a configuration entry",
    sortOptions = false
)
public class ShowCommand implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(ShowCommand.class);

    @CommandLine.Option(
        names = {"--name"},
        description = "Name of the setting",
        order = 0,
        required = false
    )
    private ConfigAttributeName name;

    @CommandLine.Option(
        names = {"--all"},
        description = "Show all the settings",
        order = 1,
        required = false
    )
    private boolean all;

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        UserSettingsFile settings = new UserSettingsFile();

        logger.exiting();
        if (all) {
            printAllSettings(settings);
            return new CommandResponse(0, "");
        }
        if (name != null) {
            String result = name.get(settings);
            printSettingValue(name.toString(), result);
            return new CommandResponse(0, "");
        }
        return new CommandResponse(0, "");
    }

    private static void printSettingValue(String setting, Object value) {
        if (value != null) {
            System.out.println(setting + ": " + value);
        } else {
            System.out.println(setting + ": not set");
        }
    }

    private static void printAllSettings(UserSettingsFile settings) {
        printSettingValue("aruRetryInterval", settings.getAruRetryInterval());
        printSettingValue("aruRetryMax", settings.getAruRetryMax());
        printSettingValue("defaultBuildPlatform", settings.getDefaultBuildPlatform());
        printSettingValue("buildContextDirectory", settings.getBuildContextDirectory());
        printSettingValue("containerEngine", settings.getContainerEngine());
        printSettingValue("installerDirectory", settings.getInstallerDirectory());
        printSettingValue("patchDirectory", settings.getPatchDirectory());
    }
}
