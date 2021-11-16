// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static com.oracle.weblogic.imagetool.util.Constants.BUSYBOX;

@Command(
    name = "createAuxImage",
    description = "Build Auxiliary docker image for WebLogic domain configuration",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class CreateAuxImage extends CommonOptions implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateAuxImage.class);

    @Override
    public CommandResponse call() throws Exception {
        Instant startTime = Instant.now();

        initializeOptions();

        // WDT install and artifacts should be defaulted to the /auxiliary directory instead of /u01/wdt
        dockerfileOptions.setWdtHome("/auxiliary");

        // if the user did not specify a fromImage, use BusyBox as the base image.
        if (Utils.isEmptyString(fromImage())) {
            dockerfileOptions.setBaseImage(BUSYBOX);
            dockerfileOptions.usingBusybox(true);
        }

        copyOptionsFromImage(fromImage(), buildDir());

        wdtOptions.handleWdtArgs(dockerfileOptions, buildDir());

        // Create Dockerfile
        String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
            "aux-image.mustache", dockerfileOptions, dryRun);

        runDockerCommand(dockerfile, getInitialBuildCmd(buildDir()));

        if (!skipcleanup) {
            Utils.deleteFilesRecursively(buildDir());
        }

        logger.exiting();
        if (dryRun) {
            return CommandResponse.success("IMG-0054");
        } else {
            return CommandResponse.success("IMG-0053",
                Duration.between(startTime, Instant.now()).getSeconds(), imageTag);
        }
    }


    @CommandLine.ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtBaseOptions wdtOptions = new WdtBaseOptions();
}
