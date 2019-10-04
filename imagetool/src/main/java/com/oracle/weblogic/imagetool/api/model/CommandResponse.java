// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

public class CommandResponse {

    private int status;
    private String message;
    private Object result;

    /**
     * For use with PicoCLI to return the response to the command line.
     * @param status CLI status, 0 if normal.
     * @param message message to the user.
     */
    public CommandResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * For use with PicoCLI to return the response to the command line.
     * @param status CLI status, 0 if normal.
     * @param message message to the user.
     * @param result more details that help the user understand the message.
     */
    public CommandResponse(int status, String message, Object result) {
        this.status = status;
        this.message = message;
        this.result = result;
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
        return message;
    }


    /**
     * Get the result in this response.
      * @param <T> result type.
     * @return the result object.
     */
    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    /**
     * True if the status was 0.
     * @return true if the status was 0.
     */
    public boolean isSuccess() {
        return status == 0;
    }

}
