// Copyright (c) 2020. 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.logging;

public enum AnsiColor {
    RESET("reset", "\u001B[0m"),

    // basic 8 colors (available on almost all basic terminals)
    BLACK("black","\u001B[30m"),
    BLUE("blue","\u001B[34m"),
    CYAN("cyan","\u001B[36m"),
    GREEN("green","\u001B[32m"),
    PURPLE("purple","\u001B[35m"),
    RED("red","\u001B[31m"),
    WHITE("white","\u001B[37m"),
    YELLOW("yellow","\u001B[33m"),

    // bright or bold versions of the basic 8
    BRIGHT_BLACK("brightblack", "\u001b[30;1m"),
    BRIGHT_BLUE("brightblue", "\u001b[34;1m"),
    BRIGHT_CYAN("brightcyan", "\u001b[36;1m"),
    BRIGHT_GREEN("brightgreen", "\u001b[32;1m"),
    BRIGHT_PURPLE("brightpurple", "\u001b[35;1m"),
    BRIGHT_RED("brightred", "\u001b[31;1m"),
    BRIGHT_WHITE("brightwhite", "\u001b[37;1m"),
    BRIGHT_YELLOW("brightyellow", "\u001b[33;1m"),

    // decorations
    BOLD("bold","\u001b[1m"),
    UNDERLINE("underline","\u001b[4m");

    private static boolean colorEnabled = true;
    private String name;
    private String value;

    AnsiColor(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Disables color logging.  These enums will resolve to empty strings.
     */
    public static void disable() {
        colorEnabled = false;
    }

    /**
     * Convert the string value to a data type enum value.
     *
     * @param name color name to search for
     * @return matching enum for provided name, or null if not found
     */
    public static AnsiColor fromValue(String name) {
        for (AnsiColor eachVal : AnsiColor.values()) {
            if (eachVal.name.equalsIgnoreCase(name)) {
                return eachVal;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (colorEnabled) {
            return value;
        } else {
            return "";
        }
    }
}
