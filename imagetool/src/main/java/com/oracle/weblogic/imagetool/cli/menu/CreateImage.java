// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
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
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(
    name = "create",
    description = "Build WebLogic docker image",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class CreateImage extends CommonCreateOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateImage.class);

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();

        try {
            initializeOptions();
            prepareNewImage();

            // build wdt args if user passes --wdtModelPath
            wdtOptions.handleWdtArgs(dockerfileOptions, buildDir());

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
                "Create_Image.mustache", dockerfileOptions, dryRun);

            runDockerCommand(dockerfile, getInitialBuildCmd(buildDir()));
            if (!dryRun) {
                wdtOptions.handleResourceTemplates(imageTag());
            }
        } catch (Exception ex) {
            logger.fine("**ERROR**", ex);
            return CommandResponse.error(ex.getMessage());
        } finally {
            if (!skipcleanup) {
                Utils.deleteFilesRecursively(buildDir());
                Utils.removeIntermediateDockerImages(buildEngine, buildId());
            }
        }
        Instant endTime = Instant.now();
        logger.exiting();
        if (dryRun) {
            return CommandResponse.success("IMG-0054");
        } else {
            return CommandResponse.success("IMG-0053", Duration.between(startTime, endTime).getSeconds(), imageTag());
        }
    }

    @ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtFullOptions wdtOptions = new WdtFullOptions();
}