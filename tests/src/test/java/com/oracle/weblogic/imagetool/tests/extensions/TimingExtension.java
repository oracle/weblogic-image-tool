// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.extensions;

import java.util.concurrent.TimeUnit;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final LoggingFacade logger = LoggingFactory.getLogger(TimingExtension.class);

    private static final String START_TIME = "start time";

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = System.currentTimeMillis() - startTime;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes);

        LoggingExtension.getLogger((context.getRequiredTestClass()))
            .info("========== Test duration [{0}] method={1}: {2} min, {3} sec ==========",
                context.getDisplayName(),
                context.getRequiredTestMethod().getName(),
                minutes, seconds);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(
            ExtensionContext.Namespace.create(context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }
}
