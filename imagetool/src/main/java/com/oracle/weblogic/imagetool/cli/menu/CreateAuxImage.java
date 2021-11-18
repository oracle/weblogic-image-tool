// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;

import static com.oracle.weblogic.imagetool.util.Constants.BUSYBOX;

@Command(
    name = "createAuxImage",
    description = "Build Auxiliary docker image for WebLogic domain configuration",
    requiredOptionMarker = '*',
    defaultValueProvider = CreateAuxImage.class,
    abbreviateSynopsis = true
)
public class CreateAuxImage extends CommonOptions
    implements Callable<CommandResponse>, IDefaultValueProvider {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateAuxImage.class);

    @Override
    public CommandResponse call() throws Exception {
        Instant startTime = Instant.now();

        try {
            initializeOptions();

            // WDT install and artifacts should be defaulted to the /auxiliary directory instead of /u01/wdt
            dockerfileOptions.setWdtHome("/auxiliary");
            // The default for Aux is busybox.  copyOptionsFromImage() will override this if --fromImage is provided.
            dockerfileOptions.usingBusybox(true);

            copyOptionsFromImage();

            wdtOptions.handleWdtArgs(dockerfileOptions, buildDir());

            // Create Dockerfile
            String dockerfile = Utils.writeDockerfile(buildDir() + File.separator + "Dockerfile",
                "aux-image.mustache", dockerfileOptions, dryRun);

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

    /**
     * Default value plugin to override the default value for --fromImage.
     *
     * @param argSpec Provided by the PicoCLI framework.
     * @return BusyBox for --fromImage default value, null otherwise.
     */
    @Override
    public String defaultValue(ArgSpec argSpec) {
        if (!argSpec.isOption()) {
            return null;
        }
        // Using the parameter label, locate --fromImage Option and change its default to busybox
        if (CommonOptions.FROM_IMAGE_LABEL.equals(argSpec.paramLabel())) {
            return BUSYBOX;
        }
        return null;
    }


    @ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtBaseOptions wdtOptions = new WdtBaseOptions();
}
