// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.extensions;

import java.util.List;

import com.oracle.weblogic.imagetool.logging.AnsiColor;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.tests.annotations.Logger;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public class LoggingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    // emphasis at the beginning and the end of a line.
    public static final String EM = "==========";
    private static final String BEGIN = EM + AnsiColor.BRIGHT_GREEN;
    private static final String FAIL = EM + AnsiColor.BRIGHT_RED;
    private static final String END = AnsiColor.RESET + EM;

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        getLogger(context.getRequiredTestClass())
            .info(BEGIN + " Starting test [{0}] method={1} " + END,
                context.getDisplayName(),
                context.getRequiredTestMethod().getName());
    }


    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        boolean testFailed = context.getExecutionException().isPresent();
        LoggingFacade logger = getLogger((context.getRequiredTestClass()));
        if (testFailed) {
            logger.severe(FAIL + " FAILED test [{0}] method={1} " + END,
                context.getDisplayName(), context.getRequiredTestMethod().getName());
            logger.severe(context.getExecutionException().get().getMessage());
        } else {
            logger.info(EM + " Finished test [{0}] method={1} " + EM,
                context.getDisplayName(), context.getRequiredTestMethod().getName());
        }
    }


    /**
     * Return the declared logger from the test class by finding the annotated logger with @Logger.
     * @param testClass JUnit5 test class
     * @return LoggingFacade instance from the test class
     */
    public static LoggingFacade getLogger(Class<?> testClass) {
        List<LoggingFacade> loggers = AnnotationSupport
            .findAnnotatedFieldValues(testClass, Logger.class, LoggingFacade.class);
        if (loggers == null || loggers.isEmpty()) {
            throw new IllegalStateException("Test class does not have an annotated LoggingFacade with @Logger : "
                + testClass.getName());
        } else if (loggers.size() > 1) {
            throw new IllegalStateException("Test class should not have more than one @Logger annotation: "
                + testClass.getName());
        }

        return loggers.get(0);
    }
}
