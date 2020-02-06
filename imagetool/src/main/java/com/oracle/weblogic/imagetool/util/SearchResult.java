// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import org.w3c.dom.Document;

public class SearchResult {
    private boolean success;
    private Document results;
    private String errorMessage;

    /**
     * Get the error errorMessage.
     *
     * @return
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error errorMessage.
     *
     * @param errorMessage message value.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get the result object from the search.
     *
     * @return dom document detailing about the conflicts
     */
    public Document getResults() {
        return results;
    }

    /**
     * Set the result object from the search.
     *
     * @param results dom document detailing about the conflicts
     */
    public void setResults(Document results) {
        this.results = results;
    }

}
