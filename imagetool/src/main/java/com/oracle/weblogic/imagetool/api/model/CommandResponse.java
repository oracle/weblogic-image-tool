// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import com.oracle.weblogic.imagetool.util.Utils;

public class CommandResponse {

    private int status;
    private String message;
    private Object result;
    private Object[] messageParams;

    /**
     * For use with PicoCLI to return the response to the command line.
     * @param status CLI status, 0 if normal.
     * @param message message to the user.
     */
    public CommandResponse(int status, String message, Object... messageParams) {
        this.status = status;
        this.message = message;
        this.messageParams = messageParams;
    }

    /**
     * Get the status code in this response.
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get the message in this response.
     * @return message to the user
     */
    public String getMessage() {
        return Utils.getMessage(message, messageParams);
    }

}
