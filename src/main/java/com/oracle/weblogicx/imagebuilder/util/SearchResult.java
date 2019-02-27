/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.util;

import org.w3c.dom.Document;

public class SearchResult {
    private boolean success;
    private Document results;
    private String errorMessage;

    /**
     * Get the error errorMessage
     *
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error errorMessage
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return true if no conflicts ; false if there is conflicts
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * @return dom document detailing about the conflicts
     */
    public Document getResults() {
        return results;
    }

    public void setResults(Document results) {
        this.results = results;
    }

}
