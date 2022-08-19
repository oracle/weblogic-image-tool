// Copyright (c) 2019, 2022, Oracle and/or its affiliates.
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

        String newOracleHome = null;
        String newJavaHome = null;

        try {
            initializeOptions();

            if (Utils.isEmptyString(sourceImage)) {
                // sourceImage is a required parameter.  This error will only occur if the user passes an empty string.
                return CommandResponse.error(Utils.getMessage("IMG-0117"));
            }

            logger.finer("IMG-0002", sourceImage);
            dockerfileOptions.setSourceImage(sourceImage);

            logger.info("IMG-0091", sourceImage);
            Properties sourceImageProperties = Utils.getBaseImageProperties(buildEngine, sourceImage,
                "/probe-env/inspect-image.sh", buildDir());

            String oldOracleHome = sourceImageProperties.getProperty("oracleHome", null);
            String oldJavaHome = sourceImageProperties.getProperty("javaHome", null);
            String domainHome = sourceImageProperties.getProperty("domainHome", null);
            String wdtHome = sourceImageProperties.getProperty("wdtHome", null);
            String modelHome = sourceImageProperties.getProperty("wdtModelHome", null);
            boolean modelOnly = Boolean.parseBoolean(sourceImageProperties.getProperty("wdtModelOnly", null));

            // If the user specified --targetImage, collect and apply the properties for the new image.
            if (!Utils.isEmptyString(targetImage)) {
                logger.finer("IMG-0002", targetImage);
                dockerfileOptions.setTargetImage(targetImage);
                dockerfileOptions.setRebaseToTarget(true);

                Properties targetImageProperties = Utils.getBaseImageProperties(buildEngine, targetImage,
                    "/probe-env/inspect-image.sh", buildDir());
                newOracleHome = targetImageProperties.getProperty("oracleHome", null);
                newJavaHome = targetImageProperties.getProperty("javaHome", null);
                useFileOwnerFromTarget(targetImageProperties);
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
                .setWdtHome(wdtHome)
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

    private void useFileOwnerFromTarget(Properties imageProperties) {
        String userid = imageProperties.getProperty("oracleHomeUser", null);
        String groupid = imageProperties.getProperty("oracleHomeGroup", null);
        if (!Utils.isEmptyString(userid)) {
            dockerfileOptions.setUserId(userid);
        }
        if (!Utils.isEmptyString(groupid)) {
            dockerfileOptions.setGroupId(groupid);
        }
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