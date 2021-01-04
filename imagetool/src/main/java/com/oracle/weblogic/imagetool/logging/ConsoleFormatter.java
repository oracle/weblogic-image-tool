// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A color-capable formatter for the console that defines a fixed output format.
 * Format is "[ LEVEL ] message", with optional throw-ables.
 */
public class ConsoleFormatter extends Formatter {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final Pattern colorPattern = Pattern.compile("\\[\\[([a-z]+): (.+?)]]");

    static {
        // if user is redirecting output, do not print color codes
        if (System.console() == null) {
            AnsiColor.disable();
        } else {
            // check logging properties to see if the user wants to disable color output
            String flag = LogManager.getLogManager().getProperty("java.util.logging.ConsoleHandler.color");
            if (flag == null) {
                String os = System.getProperty("os.name");
                if (os != null && os.toLowerCase().contains("win")) {
                    // For the Windows OS, default the color logging to disabled
                    // The Windows OS terminal does not enable color by default
                    AnsiColor.disable();
                }
                // The default for non-Windows OS's is color enabled.
            } else if (flag.equalsIgnoreCase("false")) {
                AnsiColor.disable();
            }
        }
    }

    private AnsiColor getMessageLevelColor(Level level) {
        if (level.intValue() > 800) {
            return AnsiColor.BRIGHT_RED;
        } else if (level.intValue() < 800) {
            return AnsiColor.BRIGHT_WHITE;
        } else {
            return AnsiColor.BRIGHT_BLUE;
        }
    }

    private String replaceColorTokens(String text) {
        Matcher matcher = colorPattern.matcher(text);

        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            AnsiColor replacement = AnsiColor.fromValue(matcher.group(1));
            builder.append(text, i, matcher.start());
            if (replacement == null) {
                builder.append(matcher.group(0));
            } else {
                builder.append(replacement);
                builder.append(matcher.group(2));
                builder.append(AnsiColor.RESET);
            }
            i = matcher.end();
        }
        builder.append(text.substring(i));
        return builder.toString();
    }

    /**
     * Formats the log record.
     *
     * @param record the log record
     * @return the formatted log record
     */
    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // Level
        sb.append("[")
            .append(getMessageLevelColor(record.getLevel()))
            .append(String.format("%-7s", record.getLevel().getLocalizedName()))
            .append(AnsiColor.RESET)
            .append("] ");

        // message
        sb.append(replaceColorTokens(formatMessage(record)));

        // throwable
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(LINE_SEPARATOR)
                    .append(sw.toString())
                    .append(LINE_SEPARATOR);
            } catch (Exception ex) {
                //ignore
            }
        }

        // end of line
        sb.append(AnsiColor.RESET)
            .append(LINE_SEPARATOR);
        return sb.toString();
    }

}
