// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.cache.CacheCLI;
import com.oracle.weblogic.imagetool.cli.menu.CreateImage;
import com.oracle.weblogic.imagetool.cli.menu.InspectImage;
import com.oracle.weblogic.imagetool.cli.menu.RebaseImage;
import com.oracle.weblogic.imagetool.cli.menu.UpdateImage;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ParseResult;

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
                RebaseImage.class,
                InspectImage.class,
                HelpCommand.class
        },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true,
        usageHelpWidth = 120,
        commandListHeading = "%nCommands:%n%nChoose from:%n"
)
public class ImageTool implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(ImageTool.class);

    @Override
    public CommandResponse call() {
        CommandLine.usage(ImageTool.class, System.out);
        return new CommandResponse(0, "");
    }

    /**
     * Entry point for Image Tool.
      * @param args command line arguments.
     */
    public static void main(String[] args) {
        CommandResponse response = run(new ImageTool(),
            new PrintWriter(System.out, true),
            new PrintWriter(System.err, true),
            args);

        if (response != null) {
            if (response.getStatus() != 0) {
                logger.severe(response.getMessage());
            } else if (!response.getMessage().isEmpty()) {
                logger.info(response.getMessage());
            }
            System.exit(response.getStatus());
        }
        System.exit(1);
    }

    /**
     * Used from main entry point, and also entry point for unit tests.
     * @param out where to send stdout
     * @param err where to send stderr
     * @param args the command line arguments (minus the sub commands themselves)
     */
    public static CommandResponse run(Callable<CommandResponse> callable, PrintWriter out, PrintWriter err,
                                      String... args) {

        CommandLine cmd = new CommandLine(callable)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setToggleBooleanFlags(false)
            .setUnmatchedArgumentsAllowed(false)
            .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO))
            .setOut(out)
            .setErr(err);

        cmd.execute(args);
        //Get the command line for the sub-command (if exists), and ignore the parents, like "imagetool cache listItems"
        return getSubcommand(cmd).getExecutionResult();
    }

    /**
     * Recursive method to find the deepest sub-command that was executed.
     * @param commandLine the picocli command line object to search
     * @return the lowest level command line executed
     */
    private static CommandLine getSubcommand(CommandLine commandLine) {
        ParseResult parseResult = commandLine.getParseResult();
        if (parseResult.subcommand() != null) {
            CommandLine sub = parseResult.subcommand().commandSpec().commandLine();
            return getSubcommand(sub);
        }

        return commandLine;
    }
}
