// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;

public class WLSCommandLine {

    public static <C extends Callable<T>, T> T call(C callable, boolean isCLIMode, String... args) {
        return call(callable, System.out, System.err, CommandLine.Help.Ansi.AUTO, true, isCLIMode, args);
    }

    // Turn CLIMode off if not provided
    public static <C extends Callable<T>, T> T call(C callable, String... args) {
        return call(callable, false, args);
    }

    /**
     * Access point for PicoCLI.
     * @param callable CLI driver
     * @param out output stream
     * @param err error stream
     * @param ansi ANSI mode
     * @param ignoreCaseForEnums ignore case for Enums
     * @param isCLIMode true to use WLSCommandFactory
     * @param args command line arguments
     * @param <C> Callable class
     * @param <T> Callable Type
     * @return the result
     */
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
        cmd.setUnmatchedArgumentsAllowed(false);
        //cmd.registerConverter(CacheStore.class, x -> new CacheStoreFactory().getCacheStore(x));
        List<Object> results = cmd.parseWithHandlers(new CommandLine.RunLast().useOut(out).useAnsi(ansi),
                new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(err).useAnsi(ansi), args);
        @SuppressWarnings("unchecked") T result = results == null || results.isEmpty() ? null : (T) results.get(0);
        if (result == null) {
            CommandLine.usage(callable, System.out);
            // System.exit(-1);
        }
        return result;
    }
}
