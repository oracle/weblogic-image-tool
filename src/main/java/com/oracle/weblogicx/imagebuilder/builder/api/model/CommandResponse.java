/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.api.model;

public class CommandResponse {

    private int status;
    private String message;
    private Object result;
    private boolean success = true;

    public CommandResponse(int status, String message) {
        this.status = status;
        this.message = message;

        if (status != 0) {
            this.success = false;
        }
    }

    public CommandResponse(int status, String message, Object result) {
        this.status = status;
        this.message = message;
        this.result = result;

        if (status != 0) {
            this.success = false;
        }
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
