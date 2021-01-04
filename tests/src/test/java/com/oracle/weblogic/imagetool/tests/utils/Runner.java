// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Class for executing shell commands from java.
 */
public class Runner {

    /**
     * Before running the shell command, log the command to be executed to both the PrintWriter and the Logger.
     * @param command external command to be executed
     * @param output PrintWriter to receive stdout
     * @param logger logger to use
     * @return result from running the provided command
     * @throws IOException if process start fails
     * @throws InterruptedException if the wait is interrupted before the process completes
     */
    public static CommandResult run(String command, PrintWriter output, LoggingFacade logger)
        throws IOException, InterruptedException {
        String message = "Executing command: " + command;
        logger.info(message);
        output.println();
        output.println(message);
        output.println();
        return run(command, output);
    }

    /**
     * Run the provided shell command, and send stdout to System.out.
     * @param command external command to be executed
     * @return result from running the provided command
     * @throws IOException if process start fails
     * @throws InterruptedException if the wait is interrupted before the process completes
     */
    public static CommandResult run(String command) throws IOException, InterruptedException {
        return run(command, new PrintWriter(System.out));
    }

    /**
     * Run the provided shell command, and send stdout to the PrintWriter.
     * @param command external command to be executed
     * @param output PrintWriter to receive stdout
     * @return result from running the provided command
     * @throws IOException if process start fails
     * @throws InterruptedException if the wait is interrupted before the process completes
     */
    public static CommandResult run(String command, PrintWriter output) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        Process p = null;

        try {
            pb.redirectErrorStream(true);
            Map<String,String> processEnv = pb.environment();
            processEnv.put("WLSIMG_BLDDIR", System.getProperty("WLSIMG_BLDDIR"));
            processEnv.put("WLSIMG_CACHEDIR", System.getProperty("WLSIMG_CACHEDIR"));

            p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), UTF_8));
            StringBuilder processOut = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                processOut.append(line);
                output.println(line);
            }

            p.waitFor();
            return new CommandResult(p.exitValue(), processOut.toString());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
