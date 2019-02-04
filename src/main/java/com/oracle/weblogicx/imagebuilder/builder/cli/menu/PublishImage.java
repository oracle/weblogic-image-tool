package com.oracle.weblogicx.imagebuilder.builder.cli.menu;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
        name = "publish",
        //mixinStandardHelpOptions = true,
        description = "Publish WebLogic docker image to specified docker registry",
        version = "1.0",
        sortOptions = false,
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class PublishImage implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        return null;
    }

    @Option(
            names = { "--user" },
            paramLabel = "<userId>",
            description = "Docker registry user id",
            required = true
    )
    private String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<password>",
            description = "Password for docker registry userId",
            required = true
    )
    private String password;

    @Option(
            names = { "--registry" },
            paramLabel = "<registry url>",
            description = "Registry URL to publish docker image to",
            required = true
    )
    private String registryUrl;

    @Option(
            names = { "--tag" },
            paramLabel = "TAG",
            required = true,
            description = "Tag corresponding to the image to publish. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    private String imageTag;
}
