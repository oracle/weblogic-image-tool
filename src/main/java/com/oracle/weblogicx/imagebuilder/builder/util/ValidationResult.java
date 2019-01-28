package com.oracle.weblogicx.imagebuilder.builder.util;

import org.w3c.dom.Document;

/**
 *   ValidaitonResult of patch conflicts check
 */

public class ValidationResult {
    private boolean success;
    private Document results;

    /**
     *
     * @return  true if no conflicts ; false if there is conflicts
     *
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     *
     * @return dom document detailing about the conflicts
     */
    public Document getResults() {
        return results;
    }

    public void setResults(Document results) {
        this.results = results;
    }
}
