/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.api.model;

public class CommandResponse {

    private int status;
    private String message;
    private Object result;

    public CommandResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public CommandResponse(int status, String message, Object result) {
        this.status = status;
        this.message = message;
        this.result = result;
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
}
