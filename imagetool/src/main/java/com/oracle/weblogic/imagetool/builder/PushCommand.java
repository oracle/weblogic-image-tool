// Copyright (c) 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class PushCommand extends AbstractCommand {
    private static final LoggingFacade logger = LoggingFactory.getLogger(PushCommand.class);

    private final String executable;
    private final List<String> command;

    /**
     * Create a manifest command for manipulating manifest.
     */
    public PushCommand(String buildEngine, String contextFolder) {
        Objects.requireNonNull(contextFolder);
        executable = buildEngine;
        command = new ArrayList<>();
    }


    /**
     * Add Docker image tag name for this build command.
     * @param value name to be used as the image tag.
     * @return this
     */
    public PushCommand tag(String value) {
        if (Utils.isEmptyString(value)) {
            return this;
        }
        command.add(value);
        return this;
    }


    @Override
    public List<String> getCommand(boolean showPasswords) {
        List<String> result = new ArrayList<>();
        result.add(executable);
        result.add("push");
        result.addAll(command);
        return result;
    }

    @Override
    public String toString() {
        return String.join(" ", getCommand(false));
    }

}
