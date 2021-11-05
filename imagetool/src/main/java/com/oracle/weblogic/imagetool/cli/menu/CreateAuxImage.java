// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.util.Constants.BUSYBOX;

@Command(
    name = "createAuxImage",
    description = "Build Auxiliary docker image for WebLogic domain configuration",
    requiredOptionMarker = '*',
    abbreviateSynopsis = true
)
public class CreateAuxImage  implements Callable<CommandResponse> {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CreateAuxImage.class);

    //TODO: duplicate of CommonOptions.tempDirectory
    private String tempDirectory = null;
    //TODO: duplicate of CommonOptions.nonProxyHosts
    private String nonProxyHosts = null;

    private DockerfileOptions dockerfileOptions;

    @Override
    public CommandResponse call() throws Exception {
        Instant startTime = Instant.now();

        logger.info(HelpVersionProvider.versionString());
        String buildId = UUID.randomUUID().toString();
        dockerfileOptions = new DockerfileOptions(buildId);

        // If no fromImage is provided by the user, default to using busybox
        dockerfileOptions.setBaseImage(Utils.isEmptyString(fromImage) ? BUSYBOX : fromImage);
        // WDT install and artifacts should be defaulted to the /auxiliary directory
        dockerfileOptions.setWdtHome("/auxiliary");

        copyOptionsFromImage(fromImage, buildDir());
        handleProxyUrls();
        handleChown();
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
                Duration.between(startTime, Instant.now()).getSeconds(), fromImage);
        }
    }

    //TODO: This is a duplicate of CommonOptions.getTempDirectory()
    private String buildDir() throws IOException {
        if (tempDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            tempDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", tempDirectory);
        }
        return tempDirectory;
    }

    //TODO: duplicate of CommonOptions.runDockerCommand
    private void runDockerCommand(String dockerfile, BuildCommand command) throws IOException, InterruptedException {
        logger.info("IMG-0078", command.toString());

        if (dryRun) {
            System.out.println("########## BEGIN DOCKERFILE ##########");
            System.out.println(dockerfile);
            System.out.println("########## END DOCKERFILE ##########");
        } else {
            command.run(dockerLog);
        }
    }

    //TODO: duplicate of CommonOptions.getInitialBuildCmd
    /**
     * Builds the options for docker build command.
     *
     * @return list of options
     */
    BuildCommand getInitialBuildCmd(String contextFolder) {
        logger.entering();
        BuildCommand cmdBuilder = new BuildCommand(buildEngine, contextFolder);

        cmdBuilder.forceRm(!skipcleanup)
            .tag(imageTag)
            .network(buildNetwork)
            .pull(buildPull)
            .buildArg("http_proxy", httpProxyUrl, httpProxyUrl != null && httpProxyUrl.contains("@"))
            .buildArg("https_proxy", httpsProxyUrl, httpsProxyUrl != null && httpsProxyUrl.contains("@"))
            .buildArg("no_proxy", nonProxyHosts);

        logger.exiting();
        return cmdBuilder;
    }

    //TODO: duplicate of CommonOptions.handleProxyUrls
    private void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, Constants.HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, Constants.HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    //TODO: duplicate of CommonOptions.handleChown
    private void handleChown() {
        if (osUserAndGroup == null) {
            return;
        }

        if (osUserAndGroup.length != 2) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0027"));
        }

        Pattern p = Pattern.compile("^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$");
        Matcher usr = p.matcher(osUserAndGroup[0]);
        if (!usr.matches()) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0028", osUserAndGroup[0]));
        }
        Matcher grp = p.matcher(osUserAndGroup[1]);
        if (!grp.matches()) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0029", osUserAndGroup[1]));
        }

        dockerfileOptions.setUserId(osUserAndGroup[0]);
        dockerfileOptions.setGroupId(osUserAndGroup[1]);
    }

    //TODO: duplicate of CommonOptions.copyOptionsFromImage
    private void copyOptionsFromImage(String fromImage, String tmpDir) throws IOException, InterruptedException {

        if (fromImage != null && !fromImage.isEmpty()) {
            logger.finer("IMG-0002", fromImage);
            dockerfileOptions.setBaseImage(fromImage);

            Properties baseImageProperties = Utils.getBaseImageProperties(buildEngine, fromImage,
                "/probe-env/inspect-image.sh", tmpDir);

            String existingJavaHome = baseImageProperties.getProperty("javaHome", null);
            if (existingJavaHome != null) {
                dockerfileOptions.disableJavaInstall(existingJavaHome);
                logger.info("IMG-0000", fromImage);
            }

            String existingOracleHome = baseImageProperties.getProperty("oracleHome", null);
            if (existingOracleHome != null) {
                dockerfileOptions.disableMiddlewareInstall(existingOracleHome);
                logger.info("IMG-0092", fromImage);
            }

            String pkgMgrProp = baseImageProperties.getProperty("packageManager", "YUM");

            PackageManagerType pkgMgr = PackageManagerType.valueOf(pkgMgrProp);
            logger.fine("fromImage package manager {0}", pkgMgr);
            if (packageManager != PackageManagerType.OS_DEFAULT && pkgMgr != packageManager) {
                logger.info("IMG-0079", pkgMgr, packageManager);
                pkgMgr = packageManager;
            }
            dockerfileOptions.setPackageInstaller(pkgMgr);
        } else {
            dockerfileOptions.setPackageInstaller(packageManager);
        }
    }


    @Option(
        names = {"--tag"},
        paramLabel = "TAG",
        required = true,
        description = "Tag for the final build image. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    private String imageTag;

    @Option(
        names = {"--fromImage"},
        description = "Docker image to use as base image."
    )
    private String fromImage;

    @Option(
        names = {"--dryRun"},
        description = "Skip image build execution and print Dockerfile to stdout"
    )
    boolean dryRun = false;

    //TODO: Common docker build options - should they be centralized? (network, pull, builder executable)
    @Option(
        names = {"--buildNetwork"},
        description = "Set the networking mode for the RUN instructions during build"
    )
    String buildNetwork;

    @Option(
        names = {"--pull"},
        description = "Always attempt to pull a newer version of base images during the build"
    )
    private boolean buildPull = false;

    @Option(
        names = {"--packageManager"},
        description = "Set the Linux package manager to use for installing OS packages. Default: ${DEFAULT-VALUE}"
    )
    PackageManagerType packageManager = PackageManagerType.OS_DEFAULT;

    @Option(
        names = {"--builder", "-b"},
        description = "Executable to process the Dockerfile. Default: ${DEFAULT-VALUE}"
    )
    String buildEngine = "docker";

    @Option(
        names = {"--dockerLog"},
        description = "file to log output from the docker build",
        hidden = true
    )
    private Path dockerLog;

    //TODO: common proxy options from CommonOptions
    @Option(
        names = {"--httpProxyUrl"},
        description = "proxy for http protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpProxyUrl;

    @Option(
        names = {"--httpsProxyUrl"},
        description = "proxy for https protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpsProxyUrl;

    //TODO: same as CommonOptions skipcleanup
    @Option(
        names = {"--skipcleanup"},
        description = "Do no delete build context folder, intermediate images, and failed build container."
    )
    boolean skipcleanup = false;

    //TODO: same as CommonOptions chown
    @Option(
        names = {"--chown"},
        split = ":",
        description = "userid:groupid for JDK/Middleware installs and patches. Default: oracle:oracle."
    )
    private String[] osUserAndGroup;

    @CommandLine.ArgGroup(exclusive = false, heading = "WDT Options%n")
    private final WdtBaseOptions wdtOptions = new WdtBaseOptions();
}
