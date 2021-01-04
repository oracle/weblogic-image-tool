// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.regex.Pattern;

/**
 * A simple key-value pair, where the value is a compiled regex pattern.
 */
public class NamedPattern {

    private String name;
    private Pattern pattern;

    /**
     * Create a pattern from the regex and give it a name.
     * @param name key for the pattern
     * @param regex the pattern to compile
     */
    public NamedPattern(String name, String regex) {
        if (name == null || regex == null) {
            throw new IllegalArgumentException("Neither the pattern or the name can be null");
        }
        this.name = name;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Return the name for this pattern.
     * @return the name of the pattern.
     */
    public String getName() {
        return name;
    }

    /**
     * Determine if the provided value matches the pattern stored.
     * @param value the value to compare
     * @return true if the value matches the regex (compiled pattern)
     */
    public boolean matches(String value) {
        return pattern.matcher(value).matches();
    }
}
