// Copyright (c) 2019, 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.PrintWriter;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cli.cache.CacheCLI;
import com.oracle.weblogic.imagetool.cli.config.ConfigCommand;
import com.oracle.weblogic.imagetool.cli.menu.CreateAuxImage;
import com.oracle.weblogic.imagetool.cli.menu.CreateImage;
import com.oracle.weblogic.imagetool.cli.menu.InspectImage;
import com.oracle.weblogic.imagetool.cli.menu.RebaseImage;
import com.oracle.weblogic.imagetool.cli.menu.UpdateImage;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;

import static picocli.CommandLine.ExitCode;

@Command(
        name = "imagetool",
        mixinStandardHelpOptions = true,
        description = "%nA tool to build docker images of WebLogic with selected patches and/or psu(s) applied.%n",
        versionProvider = HelpVersionProvider.class,
        sortOptions = false,
        subcommands = {
            CacheCLI.class,
            ConfigCommand.class,
            CreateImage.class,
            CreateAuxImage.class,
            UpdateImage.class,
            RebaseImage.class,
            InspectImage.class
        },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true,
        usageHelpWidth = 120,
        commandListHeading = "%nCommands:%n%nChoose from:%n"
)
public class ImageTool {

    private static final LoggingFacade logger = LoggingFactory.getLogger(ImageTool.class);

    /**
     * Entry point for Image Tool command line.
      * @param args command line arguments.
     */
    public static void main(String[] args) {
        CommandResponse response = run(ImageTool.class,
            new PrintWriter(System.out, true),
            new PrintWriter(System.err, true),
            args);

        response.logResponse(logger);
        System.exit(response.getStatus());
    }

    /**
     * Executes the provided entryPoint .
     *
     * @param entryPoint must be an instance or class annotated with picocli.CommandLine.Command
     * @param out where to send stdout
     * @param err where to send stderr
     * @param args the command line arguments (minus the sub commands themselves)
     */
    public static CommandResponse run(Object entryPoint, PrintWriter out, PrintWriter err, String... args) {
        CommandLine cmd = new CommandLine(entryPoint)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setToggleBooleanFlags(false)
            .setUnmatchedArgumentsAllowed(false)
            .setTrimQuotes(true)
            .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO))
            .setParameterExceptionHandler(new ExceptionHandler())
            .setOut(out)
            .setErr(err);

        int exit = cmd.execute(args);
        CommandResponse response;
        if (exit == ExitCode.USAGE) {
            response = new CommandResponse(ExitCode.USAGE, null);
        } else {
            CommandLine commandLine = getSubcommand(cmd);
            response = commandLine.getExecutionResult();
            if (response == null) {
                logger.fine("User requested usage by using help command");
                response = CommandResponse.success(null);
            }
        }
        //Get the command line for the sub-command (if exists), and ignore the parents, like "imagetool cache listItems"
        return response;
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

    static class ExceptionHandler implements CommandLine.IParameterExceptionHandler {

        @Override
        public int handleParseException(CommandLine.ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            PrintWriter writer = cmd.getErr();

            writer.println(ex.getMessage());
            CommandLine.UnmatchedArgumentException.printSuggestions(ex, writer);

            CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();
            ex.getCommandLine().usage(writer, cmd.getColorScheme());

            return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
        }
    }
}
