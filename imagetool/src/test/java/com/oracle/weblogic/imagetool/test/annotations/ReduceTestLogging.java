// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used in conjunction with ReduceTestLoggingExtension to disable or reduce extraneously log messages during
 * unit test execution.  The original log level will be saved before executing any JUnit tests in the annotated class,
 * and restored after all tests in the class are completed.
 * During the tests of the annotated class, the specified logger (loggerClass) will have its minimum level set to the
 * value specified in level().  The default level is OFF.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(ReduceTestLoggingExtension.class)
public @interface ReduceTestLogging {
    Class<?> loggerClass();
    LogLevel level() default LogLevel.OFF;
}
