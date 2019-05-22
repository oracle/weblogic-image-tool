/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * The Universal Permissive License (UPL), Version 1.0
 */

package com.oracle.weblogic.imagetool.logging;

import com.oracle.weblogic.imagetool.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/**
 * The java.util.logging log factory for weblogic image tool.
 */
public final class PlatformLoggingFactory {


    private static final HashMap<String, Logger> LOGGERS = new HashMap<>();
    private static Level level = Level.ALL;
    private static FileHandler debugFileHandler;
    // Hide the default constructor.
    //
    private PlatformLoggingFactory() {
        // No constructor for this utility class
    }

    public static void initializeFileHandler(Path debugLogPath) {

        if (debugFileHandler == null ) {
            try {
                Path toolLogPath = Utils.createFile(debugLogPath, "tool.log");
                debugFileHandler = new FileHandler(toolLogPath.toAbsolutePath().toString());
                debugFileHandler.setFormatter(new SimpleFormatter());
            } catch (IOException ioe) {
                System.out.println("--debugLogPath is wrong. Ignored logging settings");
            }
        }

    }

    public static FileHandler getFileHandler() {
        return debugFileHandler;
    }

    public static void cleanUp() {
        if (debugFileHandler != null) {
            debugFileHandler.close();
        }
    }

    public static void setLogLevel(String logLevel) {
        level = Level.parse(logLevel);
    }


    /**
     * Get the logger using the logger name and using the default resource bundle for the WLS Deploy tooling.
     *
     * @param loggerName the logger name
     * @return the logger
     */
    public static Logger getLogger(String loggerName) {
        Logger myLogger = LOGGERS.get(loggerName);

        if (myLogger == null) {
            myLogger = initializeLogger(loggerName);
        }
        return myLogger;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // private helper methods                                                     //
    ////////////////////////////////////////////////////////////////////////////////

    private static synchronized Logger initializeLogger(String loggerName) {
        // Make sure another thread didn't get here first and create it)
        Logger myLogger = LOGGERS.get(loggerName);
        if (myLogger == null) {
            myLogger = getComponentLogger(loggerName);
            LOGGERS.put(loggerName, myLogger);
        }
        return myLogger;
    }

    private static Logger getComponentLogger(String name) {
        final Logger logger = Logger.getLogger(name);
        if (debugFileHandler != null) {
            logger.addHandler(debugFileHandler);
        }
        if (level != null) {
            logger.setLevel(level);
        }
        return logger;
    }
}
