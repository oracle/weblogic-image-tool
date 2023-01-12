// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a simple log formatting class designed to include the ResourceBundle's MessageKey in the log.
 */
public class FileFormatter extends Formatter {
    private static final String CATALOG_KEY_PATTERN_STRING = "^[A-Z]{3,10}-[0-9]{3,5}$";
    private static final Pattern CATALOG_KEY_PATTERN = Pattern.compile(CATALOG_KEY_PATTERN_STRING);

    private static final String DATE_FORMAT_STRING = "####<{0,date,yyyy.MM.dd} {0,time,HH:mm:ss}>";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Object[] args;
    private MessageFormat formatter;
    private Date date;

    /**
     * The constructor.
     */
    public FileFormatter() {
        this.date = new Date();
        this.args = new Object[1];
        this.formatter = new MessageFormat(DATE_FORMAT_STRING);
    }

    /**
     * Remove ANSI color tokens from messages to be logged to a file.
     * @param text message to be logged
     * @return message with color tokens removed
     */
    private String replaceColorTokens(String text) {
        Matcher matcher = ConsoleFormatter.colorPattern.matcher(text);

        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            AnsiColor replacement = AnsiColor.fromValue(matcher.group(1));
            builder.append(text, i, matcher.start());
            if (replacement == null) {
                builder.append(matcher.group(0));
            } else {
                // remove color flags from messages logged to file
                builder.append(matcher.group(2));
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

        date.setTime(record.getMillis());
        args[0] = date;

        StringBuffer text = new StringBuffer();
        formatter.format(args, text, null);
        sb.append(text);

        // Level
        sb.append(" <");
        sb.append(record.getLevel().getLocalizedName());
        sb.append(">");

        // Class name
        sb.append(" <");
        String source = record.getSourceClassName();
        if (source != null) {
            sb.append(source.substring(source.lastIndexOf('.') + 1));
        } else {
            sb.append(record.getLoggerName());
        }
        sb.append(">");

        // Method name
        sb.append(" <");
        if (record.getSourceMethodName() != null) {
            sb.append(record.getSourceMethodName());
        }
        sb.append(">");

        String messageKey = record.getMessage();
        String message = replaceColorTokens(formatMessage(record));

        if (messageKey != null) {
            sb.append(" <");
            if (CATALOG_KEY_PATTERN.matcher(messageKey).matches()) {
                sb.append(messageKey);
            }
            sb.append(">");
        }
        sb.append(" <");
        sb.append(message);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(LINE_SEPARATOR);
                sb.append(sw.toString());
                sb.append(LINE_SEPARATOR);
            } catch (Exception ex) {
                //ignore
            }
        }
        sb.append(">");
        sb.append(LINE_SEPARATOR);
        return sb.toString();
    }
}
