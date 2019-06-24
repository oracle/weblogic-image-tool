/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.util;

import org.w3c.dom.Document;

/**
 * ValidaitonResult of patch conflicts check.
 */

public class ValidationResult {
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
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    /**
     * Returns true if no conflicts ; false if there is conflicts.
     * @return true if no conflicts ; false if there is conflicts.
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Get the result object from the search.
     * @return dom document detailing about the conflicts
     */
    public Document getResults() {
        return results;
    }

    public void setResults(Document results) {
        this.results = results;
    }
}
