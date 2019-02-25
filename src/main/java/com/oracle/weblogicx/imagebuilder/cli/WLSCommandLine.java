package com.oracle.weblogicx.imagebuilder.cli;

import picocli.CommandLine;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

public class WLSCommandLine {

    public static <C extends Callable<T>, T> T call(C callable, boolean isCLIMode, String... args) {
        return call(callable, System.out, System.err, CommandLine.Help.Ansi.AUTO, true, isCLIMode, args);
    }

    // Turn CLIMode off if not provided
    public static <C extends Callable<T>, T> T call(C callable, String... args) {
        return call(callable, false, args);
    }

    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, PrintStream err,
                                                    CommandLine.Help.Ansi ansi, boolean ignoreCaseForEnums,
                                                    boolean isCLIMode, String... args) {
        CommandLine cmd;
        if (isCLIMode) {
            cmd = new CommandLine(callable, new WLSCommandFactory());
        } else {
            cmd = new CommandLine(callable);
        }
        cmd.setCaseInsensitiveEnumValuesAllowed(ignoreCaseForEnums);
        cmd.setToggleBooleanFlags(false);
        cmd.setUnmatchedArgumentsAllowed(true);
        //cmd.registerConverter(CacheStore.class, x -> new CacheStoreFactory().getCacheStore(x));

        List<Object> results = cmd.parseWithHandlers(new CommandLine.RunLast().useOut(out).useAnsi(ansi),
                new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(err).useAnsi(ansi), args);
        @SuppressWarnings("unchecked") T result = results == null || results.isEmpty() ? null : (T) results.get(0);
        return result;
    }
}
