// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.cache.CacheCLI;
import com.oracle.weblogic.imagetool.cli.menu.CreateImage;
import com.oracle.weblogic.imagetool.cli.menu.UpdateImage;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
        name = "imagetool",
        mixinStandardHelpOptions = true,
        description = "%nA tool to build docker images of WebLogic with selected patches and/or psu(s) applied.%n",
        versionProvider = HelpVersionProvider.class,
        sortOptions = false,
        subcommands = {
                CacheCLI.class,
                CreateImage.class,
                UpdateImage.class,
                HelpCommand.class
        },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true,
        usageHelpWidth = 120,
        commandListHeading = "%nCommands:%n%nChoose from:%n"
)
public class CLIDriver implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CLIDriver.class);

    @Override
    public CommandResponse call() {
        return null;
    }

    /**
     * Entry point for Image Tool.
      * @param args command line arguments.
     */
    public static void main(String[] args) {
        CLIDriver cliDriver = new CLIDriver();
        if (args.length == 0) {
            CommandLine.usage(cliDriver, System.out);
            System.exit(-1);
        } else {
            CommandResponse response = WLSCommandLine.call(cliDriver, true, args);
            if (response != null) {
                if (response.getStatus() != 0) {
                    String message = String.format("Response code: %d, message: %s", response.getStatus(),
                            response.getMessage());
                    logger.severe(message);
                }
                System.exit(response.getStatus());
            }
            System.exit(-1);
        }
    }
}
