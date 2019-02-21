package com.oracle.weblogicx.imagebuilder.cli;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.cli.menu.CreateImage;
import com.oracle.weblogicx.imagebuilder.cli.menu.UpdateImage;
import com.oracle.weblogicx.imagebuilder.cli.cache.CacheCLI;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public CommandResponse call() throws Exception {
        return null;
    }

    public static void main(String[] args) {
        CLIDriver cliDriver = new CLIDriver();
        if (args.length == 0) {
            CommandLine.usage(cliDriver, System.out);
        } else {
            List<String> argsList = Stream.of(args).collect(Collectors.toList());
            argsList.add(Constants.CLI_OPTION);
            CommandResponse response = WLSCommandLine.call(cliDriver, argsList.toArray(new String[0]));
            if (response != null) {
                System.out.println(String.format("Response code: %d, message: %s", response.getStatus(),
                        response.getMessage()));
            }/* else {
                System.out.println("response is null");
            }*/
        }
    }
}
