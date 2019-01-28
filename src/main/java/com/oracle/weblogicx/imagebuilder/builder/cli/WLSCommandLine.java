package com.oracle.weblogicx.imagebuilder.builder.cli;

import picocli.CommandLine;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

public class WLSCommandLine {

    public static <C extends Callable<T>, T> T call(C callable, String... args) {
        return call(callable, System.out, System.err, CommandLine.Help.Ansi.AUTO, true, args);
    }

    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, PrintStream err,
                                                    CommandLine.Help.Ansi ansi, boolean ignoreCaseForEnums,
                                                    String... args) {
        CommandLine cmd = new CommandLine(callable);
        cmd.setCaseInsensitiveEnumValuesAllowed(ignoreCaseForEnums);
        List<Object> results = cmd.parseWithHandlers(new CommandLine.RunLast().useOut(out).useAnsi(ansi),
                new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(err).useAnsi(ansi), args);
        @SuppressWarnings("unchecked") T result = results == null || results.isEmpty() ? null : (T) results.get(0);
        return result;
    }
}
