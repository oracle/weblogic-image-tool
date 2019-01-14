package com.oracle.weblogicx.imagebuilder.builder.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Command(
        name = "builder",
        mixinStandardHelpOptions = true,
        description = "Build WebLogic docker image",
        version = "1.0",
        sortOptions = false,
        subcommands = { CacheCLI.class },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class BuilderCLIDriver implements Runnable {

    enum InstallerType { wls, fmw }

    @Option(
            names = { "--installerType" },
            description = "Installer type. Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private InstallerType installerType;

    @Option(
            names = { "--installerVersion" },
            description = "Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "12.2.1.3.0",
            completionCandidates = VersionValues.class
    )
    private String installerVersion;

    @Option(
            names = { "--latestPSU" },
            description = "Whether to apply patches from latest PSU. Mutually Exclusive to parameter -patches."
    )
    private boolean latestPSU = false;

    @Option(
            names = { "--patches" },
            paramLabel = "patchId",
            split = ",",
            description = "Comma separated patch Ids. Ex: p12345678,p87654321"
    )
    private List<String> patches;

    @Option(
            names = { "--fromImage" },
            description = "Your WebLogic docker image to use as base image."
    )
    private String fromImage;

    @Option(
            names = { "--tag" },
            paramLabel = "TAG",
            required = true,
            description = "Tag for the final build image. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    private String imageTag;

    @Option(
            names = { "--user" },
            paramLabel = "<support email>",
            required = true,
            description = "Your Oracle Support email id"
    )
    private String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<Wait for Prompt>",
            interactive = true,
            required = true,
            description = "Password for support userId"
    )
    private String password;

    @Option(
            hidden = true,
            names = { "--publish" },
            description = "Publish this docker image"
    )
    private boolean isPublish = false;

    static class VersionValues extends ArrayList<String> {
        VersionValues() {
            super(Arrays.asList("12.2.1.3.0", "12.2.1.2.0"));
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            CommandLine.usage(new BuilderCLIDriver(), System.out);
        } else {
            CommandLine.run(new BuilderCLIDriver(), args);
        }
    }

    @Override
    public void run() {
        System.out.println("hello");
        System.out.println("InstallerType = \"" + installerType + "\"");
        System.out.println("InstallerVersion = \"" + installerVersion + "\"");
        System.out.println("latestPSU = \"" + latestPSU + "\"");
        System.out.println("patches = \"" + patches + "\"");
        System.out.println("fromImage = \"" + fromImage + "\"");
        System.out.println("userId = \"" + userId + "\"");
        System.out.println("password = \"" + password + "\"");
        System.out.println("publish = \"" + isPublish + "\"");
    }
}
