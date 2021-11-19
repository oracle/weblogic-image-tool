// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "rebase",
    description = "Copy domain from one image to another",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class RebaseImage extends CommonCreateOptions implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(RebaseImage.class);

    @Override
    public CommandResponse call() throws Exception {
        logger.entering();
        Instant startTime = Instant.now();

        String oldOracleHome;
        String oldJavaHome;
        String newOracleHome = null;
        String newJavaHome = null;
        String domainHome;
        String modelHome;
        boolean modelOnly;

        try {
            initializeOptions();

            if (sourceImage != null && !sourceImage.isEmpty()) {
                logger.finer("IMG-0002", sourceImage);
                dockerfileOptions.setSourceImage(sourceImage);

                logger.info("IMG-0091", sourceImage);
                Properties baseImageProperties = Utils.getBaseImageProperties(buildEngine, sourceImage,
                    "/probe-env/inspect-image.sh", buildDir());

                oldOracleHome = baseImageProperties.getProperty("oracleHome", null);
                oldJavaHome = baseImageProperties.getProperty("javaHome", null);
                domainHome = baseImageProperties.getProperty("domainHome", null);
                modelHome = baseImageProperties.getProperty("wdtModelHome", null);
                modelOnly = Boolean.parseBoolean(baseImageProperties.getProperty("wdtModelOnly", null));
            } else {
                return CommandResponse.error("Source Image not set");
            }

            if (!Utils.isEmptyString(targetImage)) {
                logger.finer("IMG-0002", targetImage);
                dockerfileOptions.setTargetImage(targetImage);
                dockerfileOptions.setRebaseToTarget(true);

                Properties baseImageProperties = Utils.getBaseImageProperties(buildEngine, targetImage,
                    "/probe-env/inspect-image.sh", buildDir());

                newOracleHome = baseImageProperties.getProperty("oracleHome", null);
                newJavaHome = baseImageProperties.getProperty("javaHome", null);

            } else {
                dockerfileOptions.setRebaseToNew(true);
            }

            if (newJavaHome != null && !newJavaHome.equals(oldJavaHome)) {
                return CommandResponse.error(Utils.getMessage("IMG-0026"));
            }

            if (newOracleHome != null && !newOracleHome.equals(oldOracleHome)) {
                return CommandResponse.error(Utils.getMessage("IMG-0021"));
            }

            if (Utils.isEmptyString(domainHome)) {
                return CommandResponse.error(Utils.getMessage("IMG-0025"));
            }

            if (modelOnly) {
                logger.info("IMG-0090", domainHome);
                if (Utils.isEmptyString(modelHome)) {
                    logger.info("IMG-0089", dockerfileOptions.wdt_model_home());
                }
            }

            dockerfileOptions
                .setDomainHome(domainHome)
                .setWdtModelHome(modelHome)
                .setWdtModelOnly(modelOnly);

            if (dockerfileOptions.isRebaseToNew()) {
                prepareNewImage();
            }

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
                "Rebase_Image.mustache", dockerfileOptions, dryRun);

            // add directory to pass the context
            runDockerCommand(dockerfile, getInitialBuildCmd(buildDir()));
        } catch (Exception ex) {
            logger.fine("**ERROR**", ex);
            return CommandResponse.error(ex.getMessage());
        } finally {
            cleanup();
        }
        logger.exiting();
        return successfulBuildResponse(startTime);
    }

    @Option(
        names = {"--sourceImage"},
        required = true,
        description = "Docker image containing source domain."
    )
    private String sourceImage;

    @Option(
        names = {"--targetImage"},
        description = "Docker image with updated JDK or MW Home"
    )
    private String targetImage;

}