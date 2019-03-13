/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.cli.cache.CacheCLI;
import com.oracle.weblogicx.imagebuilder.cli.menu.CreateImage;
import com.oracle.weblogicx.imagebuilder.cli.menu.UpdateImage;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.util.concurrent.Callable;

@Command(
        name = "imagebuilder",
        mixinStandardHelpOptions = true,
        description = "%nA tool to build docker images of WebLogic with selected " +
                "patches and/or psu(s) applied.%n",
        version = "1.0",
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

    @Override
    public CommandResponse call() {
        return null;
    }

    public static void main(String[] args) {
        CLIDriver cliDriver = new CLIDriver();
        if (args.length == 0) {
            CommandLine.usage(cliDriver, System.out);
            System.exit(-1);
        } else {
            //List<String> argsList = Stream.of(args).collect(Collectors.toList());
            //argsList.add("--cli");
            //CommandResponse response = WLSCommandLine.call(cliDriver, argsList.toArray(new String[0]));
            CommandResponse response = WLSCommandLine.call(cliDriver, true, args);

            if (response != null) {
                System.out.println(String.format("Response code: %d, message: %s", response.getStatus(),
                        response.getMessage()));
                //Map<String, String> results = response.getResult();
                //results.forEach((key, value) -> System.out.println(key + "=" + value));
                System.exit(response.getStatus());
            }
            System.exit(-1);
        }
    }
}
