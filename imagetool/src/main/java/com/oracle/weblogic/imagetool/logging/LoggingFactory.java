// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LoggingFactory {
    // map from resourceBundleName to facade
    private static final Map<String, LoggingFacade> facade = new HashMap<>();

    private LoggingFactory() {
        // hide implicit public constructor
    }

    /**
     * Obtains a Logger from the underlying logging implementation and wraps it in a LoggingFacade.
     * Infers caller class and bundle name.
     *
     * @param clazz use class name as logger name
     * @return a PlatformLogger object for the caller to use
     */
    public static LoggingFacade getLogger(Class<?> clazz) {
        return getLogger(clazz.getName(), "ImageTool");
    }

    /**
     * Obtains a Logger from the underlying logging implementation and wraps it in a LoggingFacade.
     * Infers caller class and bundle name.
     *
     * @param name logger name
     * @return a PlatformLogger object for the caller to use
     */
    public static LoggingFacade getLogger(String name) {
        return getLogger(name, "ImageTool");
    }

    /**
     * Obtains a Logger from the underlying logging implementation and wraps it in a LoggingFacade.
     *
     * @param name the name of the logger to use
     * @param resourceBundleName the resource bundle to use with this logger
     * @return a PlatformLogger object for the caller to use
     */
    public static synchronized LoggingFacade getLogger(String name, String resourceBundleName) {

        return facade.computeIfAbsent(resourceBundleName,
            k -> new LoggingFacade(Logger.getLogger(name, resourceBundleName)));
    }
}
