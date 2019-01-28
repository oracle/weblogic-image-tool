package com.oracle.weblogicx.imagebuilder.builder.cli;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.cli.cache.CacheCLI;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(
        name = "builder",
        mixinStandardHelpOptions = true,
        description = "%nImageBuilder is a tool to help build docker images of WebLogic with selected " +
                      "patches and/or psu(s) applied.%n",
        version = "1.0",
        sortOptions = false,
        subcommands = { CacheCLI.class, BuilderCLIDriver.class },
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
            argsList.add("--cli");
//            CommandLine commandLine = new CommandLine(new BuilderCLIDriver())
//                    .setCaseInsensitiveEnumValuesAllowed(true)
//                    .setUsageHelpWidth(120);
//
//            List<Object> results = commandLine.parseWithHandlers(
//                    new CommandLine.RunLast().useOut(System.out).useAnsi(CommandLine.Help.Ansi.AUTO),
//                    new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(System.err).useAnsi(CommandLine.Help.Ansi.AUTO),
//                    argsList.toArray(new String[0]));
//
//            CommandResponse response = (results == null || results.isEmpty())? null : (CommandResponse) results.get(0);

            CommandResponse response = WLSCommandLine.call(cliDriver, argsList.toArray(new String[0]));
            if (response != null) {
                System.out.println(String.format("Response code: %d, message: %s", response.getStatus(),
                        response.getMessage()));
            } else {
                System.out.println("response is null");
            }
        }
    }
}
