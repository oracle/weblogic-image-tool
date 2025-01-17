// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.CloseableList;
import com.oracle.weblogic.imagetool.util.Utils;

public abstract class AbstractCommand {
    private static final LoggingFacade logger = LoggingFactory.getLogger(AbstractCommand.class);

    public abstract List<String> getCommand(boolean showPasswords);

    /**
     * Executes the given docker command and writes the process stdout to log.
     *
     * @param dockerLog      log file to write to
     * @throws IOException          if an error occurs reading from the process inputstream.
     * @throws InterruptedException when the process wait is interrupted.
     */
    public void run(Path dockerLog)
        throws IOException, InterruptedException {
        // process builder
        logger.entering(getCommand(false), dockerLog);
        Path dockerLogPath = createFile(dockerLog);
        logger.finer("Docker log: {0}", dockerLogPath);

        if (dockerLogPath != null) {
            logger.info("dockerLog: " + dockerLog);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(getCommand(true));
        processBuilder.redirectErrorStream(true);
        logger.finer("Starting docker process...");
        final Process process = processBuilder.start();
        logger.finer("Docker process started");
        Thread readerThread = writeFromInputToOutputStreams(process.getInputStream(), dockerLogPath);
        logger.finer("Waiting for Docker to finish");
        if (process.waitFor() != 0) {
            Utils.processError(process);
        }
        readerThread.join();
    }

    /**
     * Create a file with the given path.
     *
     * @param filePath        the path of the file to create
     * @return file path or null in case of error
     */
    private Path createFile(Path filePath) {
        Path logFilePath = filePath;
        if (logFilePath != null) {
            try {
                if (!Files.exists(logFilePath)) {
                    Files.createDirectories(logFilePath.getParent());
                    Files.createFile(logFilePath);
                } else {
                    if (Files.isDirectory(logFilePath)) {
                        logFilePath = Paths.get(logFilePath.toAbsolutePath().toString(), "dockerbuild.log");
                        if (Files.exists(logFilePath)) {
                            Files.delete(logFilePath);
                        }
                        Files.createFile(logFilePath);
                    }
                }
            } catch (IOException e) {
                logger.fine("Failed to create log file for the build command", e);
                logFilePath = null;
            }
        }
        return logFilePath;
    }

    private Thread writeFromInputToOutputStreams(InputStream inputStream, Path dockerLogPath) {
        Thread readerThread = new Thread(() -> {
            BufferedReader processReader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter stdoutWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)),
                true);
            PrintWriter logWriter = null;
            OutputStream fileOutputStream = null;

            try  {
                if (dockerLogPath != null) {
                    fileOutputStream = Files.newOutputStream(dockerLogPath);
                    logWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fileOutputStream)),
                        true);
                }
                String line;
                while ((line = processReader.readLine()) != null) {
                    if (logWriter != null) {
                        logWriter.println(line);
                    }
                    stdoutWriter.println(line);
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
            } finally {
                try {
                    processReader.close();
                    if (logWriter != null) {
                        logWriter.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException ioe) {
                    logger.finest(ioe.getMessage());
                }
            }
        });
        readerThread.start();
        return readerThread;
    }

    private CloseableList<PrintWriter> createPrintWriters(List<OutputStream> outputStreams) {
        CloseableList<PrintWriter> retVal = new CloseableList<>();
        for (OutputStream outputStream : outputStreams) {
            retVal.add(new PrintWriter(new OutputStreamWriter(outputStream), true));
        }
        return retVal;
    }

}
