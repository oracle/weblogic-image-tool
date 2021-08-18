// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.util.Utils;

import static picocli.CommandLine.ExitCode;

public class CommandResponse {

    private final int status;
    private final String message;
    private final Object[] messageParams;

    /**
     * For use with PicoCLI to return the response to the command line.
     *
     * @param status  CLI status, 0 if normal.
     * @param message message to the user.
     */
    public CommandResponse(int status, String message, Object... messageParams) {
        this.status = status;
        this.message = message;
        this.messageParams = messageParams;
    }

    /**
     * Create a new error response (status 1).
     *
     * @param message       error message
     * @param messageParams parameters for the error message
     * @return a new CommandResponse with status 1
     */
    public static CommandResponse error(String message, Object... messageParams) {
        return new CommandResponse(ExitCode.SOFTWARE, message, messageParams);
    }

    /**
     * Create a new success response (status 0).
     *
     * @param message       optional message
     * @param messageParams parameters for the message
     * @return a new CommandResponse with status 1
     */
    public static CommandResponse success(String message, Object... messageParams) {
        return new CommandResponse(ExitCode.OK, message, messageParams);
    }
    
    /**
     * Get the status code in this response.
     *
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get the message in this response.
     *
     * @return message to the user
     */
    public String getMessage() {
        return Utils.getMessage(message, messageParams);
    }

    /**
     * Log the response message with the provided logger.
     *
     * @param logger logger to use.
     */
    public void logResponse(LoggingFacade logger) {
        if (Utils.isEmptyString(message)) {
            return;
        }

        if (status == ExitCode.OK) {
            logger.info(getMessage());
        } else if (status == ExitCode.SOFTWARE) {
            logger.severe(getMessage());
        }
    }
}
