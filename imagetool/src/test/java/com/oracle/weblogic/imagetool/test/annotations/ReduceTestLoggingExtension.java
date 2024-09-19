// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.test.annotations;

import java.util.Optional;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Used in conjunction with ReduceTestLogging to disable or reduce extraneously log messages during
 * unit test execution.
 */
public class ReduceTestLoggingExtension implements BeforeAllCallback, AfterAllCallback {
    private static LoggingFacade logger;
    private static Level oldLevel;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Optional<Class<?>> testClass = extensionContext.getTestClass();
        if (testClass.isPresent()) {
            ReduceTestLogging annotation = testClass.get().getAnnotation(ReduceTestLogging.class);
            logger = LoggingFactory.getLogger(annotation.loggerClass());
            oldLevel = logger.getLevel();
            logger.setLevel(annotation.level().value());
        } else {
            throw new IllegalArgumentException("TestLoggerExtension can not be used outside of TestLoggerOverride.");
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        logger.setLevel(oldLevel);
    }
}
