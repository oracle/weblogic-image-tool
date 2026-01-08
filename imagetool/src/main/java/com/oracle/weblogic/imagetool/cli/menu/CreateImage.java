// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.builder.AbstractCommand;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.builder.ManifestCommand;
import com.oracle.weblogic.imagetool.builder.PushCommand;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(
    name = "create",
    description = "Build WebLogic docker image",
    requiredOptionMarker = '*',
    showEndOfOptionsDelimiterInUsageHelp = true,
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
            wdtOptions.handleWdtArgs(dockerfileOptions, buildDir(), getTargetArchitecture());

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
                "Create_Image.mustache", dockerfileOptions, dryRun);
            BuildCommand buildCommand = getInitialBuildCmd(buildDir());
            if ("podman".equalsIgnoreCase(buildCommand.getExecutable())) {
                List<AbstractCommand> abstractCommands =  new ArrayList<>();
                List<String> buildPlatforms = buildCommand.getBuildPlatforms();
                if (buildPlatforms.size() > 1) {
                    // build each platform first, then build manifest and the push the manifest if needed...
                    ManifestCommand createManifestCommand = new ManifestCommand(buildCommand.getExecutable(),
                        buildCommand.getContext());
                    String manifestName = buildCommand.getTagName();
                    createManifestCommand.create().name(manifestName);
                    abstractCommands.add(createManifestCommand);
                    logger.info("IMG-0140", manifestName);

                    for (String buildPlatform : buildPlatforms) {
                        buildCommand.substitutePlatform(buildPlatform);
                        String platformTag = buildCommand.substituteTagName(imageTag + "-"
                            + buildPlatform.replace('/', '-'));
                        runDockerCommand(dockerfile, buildCommand);
                        PushCommand pushCommand = new PushCommand(buildCommand.getExecutable(),
                            buildCommand.getContext()).tag(platformTag);
                        abstractCommands.add(pushCommand);
                        logger.info("IMG-0143", platformTag);

                        ManifestCommand addManifestCommand = new ManifestCommand(buildCommand.getExecutable(),
                            buildCommand.getContext());
                        addManifestCommand.add().name(manifestName).tag(platformTag);
                        abstractCommands.add(addManifestCommand);
                        logger.info("IMG-0141", manifestName, platformTag);
                    }

                    ManifestCommand pushManifestCommand = new ManifestCommand(buildCommand.getExecutable(),
                        buildCommand.getContext());
                    pushManifestCommand.push().name(manifestName).tag(manifestName);
                    abstractCommands.add(pushManifestCommand);
                    logger.info("IMG-0142", manifestName);
                    for (AbstractCommand abstractCommand : abstractCommands) {
                        logger.info("IMG-0144", abstractCommand.toString());
                        runDockerCommand(dockerfile, abstractCommand);
                    }

                } else {
                    runDockerCommand(dockerfile, buildCommand);
                }
            } else {
                runDockerCommand(dockerfile, buildCommand);
            }
            if (!dryRun) {
                wdtOptions.handleResourceTemplates(imageTag());
            }
        } catch (Exception ex) {
            logger.fine("**ERROR**", ex);
            return CommandResponse.error(ex.getMessage());
        } finally {
            cleanup();
        }
        logger.exiting();
        return successfulBuildResponse(startTime);
    }


    @ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtFullOptions wdtOptions = new WdtFullOptions();
}