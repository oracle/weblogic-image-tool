// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;

public class WLSCommandLine {

    public static <C extends Callable<T>, T> T call(C callable, String... args) {
        return call(callable, System.out, System.err, CommandLine.Help.Ansi.AUTO, true, args);
    }

    /**
     * Access point for PicoCLI.
     * @param callable CLI driver
     * @param out output stream
     * @param err error stream
     * @param ansi ANSI mode
     * @param ignoreCaseForEnums ignore case for Enums
     * @param args command line arguments
     * @param <C> Callable class
     * @param <T> Callable Type
     * @return the result
     */
    public static <C extends Callable<T>, T> T call(C callable, PrintStream out, PrintStream err,
                                                    CommandLine.Help.Ansi ansi, boolean ignoreCaseForEnums,
                                                    String... args) {
        CommandLine cmd;
        cmd = new CommandLine(callable);
        cmd.setCaseInsensitiveEnumValuesAllowed(ignoreCaseForEnums);
        cmd.setToggleBooleanFlags(false);
        cmd.setUnmatchedArgumentsAllowed(false);
        List<Object> results = cmd.parseWithHandlers(new CommandLine.RunLast().useOut(out).useAnsi(ansi),
                new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(err).useAnsi(ansi), args);
        @SuppressWarnings("unchecked") T result = results == null || results.isEmpty() ? null : (T) results.get(0);
        if (result == null) {
            CommandLine.usage(callable, out);
            // System.exit(-1);
        }
        return result;
    }
}
