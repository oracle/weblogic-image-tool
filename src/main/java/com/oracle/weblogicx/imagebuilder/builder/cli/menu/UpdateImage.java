/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli.menu;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "update",
        description = "Update WebLogic docker image with selected patches",
        version = "1.0",
        sortOptions = false,
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage implements Callable<CommandResponse> {
    @Override
    public CommandResponse call() throws Exception {
        return new CommandResponse(0, "work in progress");
    }
}
