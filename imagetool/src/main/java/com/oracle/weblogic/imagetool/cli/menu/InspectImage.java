// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.inspect.InspectOutput;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine;

@CommandLine.Command(
    name = "inspect",
    description = "Inspect an image created by this tool",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class InspectImage implements Callable<CommandResponse> {
    @Override
    public CommandResponse call() throws Exception {
        Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
        String tempDirectory = tmpDir.toAbsolutePath().toString();

        String scriptToRun = "/probe-env/inspect-image.sh";
        // add additional formats here, the ENUM, and to the resources/inspect-responses folder
        if (listPatches) {
            scriptToRun = "/probe-env/inspect-image-long.sh";
        }

        Properties baseImageProperties =
            Utils.getBaseImageProperties(buildEngine, imageName, scriptToRun, tempDirectory);

        System.out.println(new InspectOutput(baseImageProperties));

        return new CommandResponse(0, "");
    }

    @CommandLine.Option(
        names = {"--image", "-i"},
        required = true,
        paramLabel = "IMAGE:ID",
        description = "Image ID or image name to be inspected"
    )
    private String imageName;

    @CommandLine.Option(
        names = {"--builder", "-b"},
        description = "Executable to inspect docker images. Default: ${DEFAULT-VALUE}"
    )
    String buildEngine = "docker";

    @CommandLine.Option(
        names = {"--patches"},
        description = "Include OPatch information in the output, including a list of patches applied.",
        defaultValue = "false"
    )
    private boolean listPatches;

    @CommandLine.Option(
        names = {"--format"},
        paramLabel = "FORMAT",
        description = "Output format. Supported values: ${COMPLETION-CANDIDATES} Default: ${DEFAULT-VALUE}",
        defaultValue = "JSON"
    )
    private OutputFormat outputFormat;
}
