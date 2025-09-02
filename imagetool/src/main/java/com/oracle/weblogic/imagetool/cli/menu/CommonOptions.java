// Copyright (c) 2019, 2025, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.builder.AbstractCommand;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
import com.oracle.weblogic.imagetool.inspect.OperatingSystemProperties;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.settings.ConfigManager;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Architecture;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.InvalidPatchIdFormatException;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import static com.oracle.weblogic.imagetool.util.Constants.AMD64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.ARM64_BLD;
import static com.oracle.weblogic.imagetool.util.Constants.BUSYBOX_OS_IDS;
import static com.oracle.weblogic.imagetool.util.Constants.CTX_FMW;
import static com.oracle.weblogic.imagetool.util.Constants.CTX_JDK;

public abstract class CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonOptions.class);
    private static final String FILESFOLDER = "files";
    public static final String FROM_IMAGE_LABEL = "<image name>";

    DockerfileOptions dockerfileOptions;
    private String buildDirectory = null;
    private String nonProxyHosts = null;
    private String buildId;

    private void handleChown() {
        if (!isChownSet()) {
            // nothing to do, user did not specify --chown on the command line
            return;
        }

        String[] userGroupPair = osUserAndGroup.split(":");
        if (userGroupPair.length != 2) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0027"));
        }

        Pattern p = Pattern.compile("^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$");
        Matcher usr = p.matcher(userGroupPair[0]);
        if (!usr.matches()) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0028", userGroupPair[0]));
        }
        Matcher grp = p.matcher(userGroupPair[1]);
        if (!grp.matches()) {
            throw new IllegalArgumentException(Utils.getMessage("IMG-0029", userGroupPair[1]));
        }

        dockerfileOptions.setUserId(userGroupPair[0]);
        dockerfileOptions.setGroupId(userGroupPair[1]);
    }

    private void handleAdditionalBuildCommands() throws IOException {
        if (additionalBuildCommandsPath != null) {
            if (!Files.isRegularFile(additionalBuildCommandsPath)) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0030", additionalBuildCommandsPath));
            }

            AdditionalBuildCommands additionalBuildCommands = new AdditionalBuildCommands(additionalBuildCommandsPath);
            dockerfileOptions.setAdditionalBuildCommands(additionalBuildCommands.getContents(dockerfileOptions));
        }

        if (additionalBuildFiles != null) {
            Files.createDirectory(Paths.get(buildDir(), FILESFOLDER));
            for (Path additionalFile : additionalBuildFiles) {
                if (!Files.isReadable(additionalFile)) {
                    throw new FileNotFoundException(Utils.getMessage("IMG-0030", additionalFile));
                }
                Path targetFile = Paths.get(buildDir(), FILESFOLDER, additionalFile.getFileName().toString());
                logger.info("IMG-0043", additionalFile);
                if (Files.isDirectory(additionalFile)) {
                    Utils.copyLocalDirectory(additionalFile, targetFile);
                } else {
                    Utils.copyLocalFile(additionalFile, targetFile);
                }
            }
        }
    }

    void runDockerCommand(String dockerfile, AbstractCommand command) throws IOException, InterruptedException {
        logger.info("IMG-0078", command.toString());

        if (dryRun) {
            System.out.println("########## BEGIN DOCKERFILE ##########");
            System.out.println(dockerfile);
            System.out.println("########## END DOCKERFILE ##########");
        } else {
            command.run(dockerLog);
        }
    }


    /**
     * Builds the options for docker build command.
     *
     * @return list of options
     */
    BuildCommand getInitialBuildCmd(String contextFolder) {
        logger.entering();

        if (buildPlatform == null) {
            buildPlatform = new ArrayList<>();
        }
        if (buildPlatform.isEmpty()) {
            ConfigManager configManager = ConfigManager.getInstance();
            if (configManager.getDefaultBuildPlatform() != null) {
                buildPlatform.add(configManager.getDefaultBuildPlatform());
            } else {
                buildPlatform.add(Architecture.getLocalArchitecture().name());
            }
        }


        BuildCommand cmdBuilder = new BuildCommand(buildEngine, contextFolder)
            .useBuildx(useBuildx)
            .forceRm(!skipcleanup)
            .tag(imageTag)
            .platform(buildPlatform)
            .network(buildNetwork)
            .pull(buildPull)
            .additionalOptions(buildOptions)
            .buildArg(buildArgs)
            .buildArg("http_proxy", httpProxyUrl, httpProxyUrl != null && httpProxyUrl.contains("@"))
            .buildArg("https_proxy", httpsProxyUrl, httpsProxyUrl != null && httpsProxyUrl.contains("@"))
            .buildArg("no_proxy", nonProxyHosts);

        // if it is multiplatform build and using docker

        if (buildId != null && buildPlatform.size() > 1) {
            // if push is specified, ignore load value
            if (!buildEngine.equalsIgnoreCase("podman")) {
                if (push) {
                    cmdBuilder.push(push);
                } else if (load) {
                    cmdBuilder.load(load);
                } else {
                    cmdBuilder.push(true);
                }
            }
        }
        logger.exiting();
        return cmdBuilder;
    }

    private void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, Constants.HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, Constants.HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    String buildDir() throws IOException {
        if (buildDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            buildDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", buildDirectory);
        }
        Path fmwamd64 = Paths.get(CTX_FMW + AMD64_BLD);
        Path fmwarm64 = Paths.get(CTX_FMW + ARM64_BLD);
        Path jdkamd64 = Paths.get(CTX_JDK + AMD64_BLD);
        Path jdkarm64 = Paths.get(CTX_JDK + ARM64_BLD);

        if (!Files.exists(fmwamd64)) {
            Files.createDirectories(Paths.get(buildDirectory).resolve(fmwamd64));
        }
        if (!Files.exists(fmwarm64)) {
            Files.createDirectories(Paths.get(buildDirectory).resolve(fmwarm64));
        }
        if (!Files.exists(jdkamd64)) {
            Files.createDirectories(Paths.get(buildDirectory).resolve(jdkamd64));
        }
        if (!Files.exists(jdkarm64)) {
            Files.createDirectories(Paths.get(buildDirectory).resolve(jdkarm64));
        }

        return buildDirectory;
    }

    /**
     * Override the default behavior for generating a temp directory.
     * This method is used by UNIT tests ONLY.
     * @param value should be the value of a temp directory generated by the UNIT test framework
     */
    void setBuildDirectory(String value) {
        buildDirectory = value;
    }

    /**
     * Apply command line values associated with CommonOptions to Dockerfile options.
     * @throws IOException thrown if WIT version cannot be retrieved, or Proxy URLs cannot be read, or
     *                     provided additional build commands file cannot be read.
     * @throws InvalidCredentialException Overriding classes may throw this exception
     * @throws InvalidPatchIdFormatException Overriding classes may throw this exception
     */
    void initializeOptions() throws InvalidCredentialException, IOException, InvalidPatchIdFormatException {
        logger.entering();
        buildId = UUID.randomUUID().toString();
        logger.info(HelpVersionProvider.versionString());
        logger.info("IMG-0016", buildId);
        dockerfileOptions = new DockerfileOptions(buildId);
        dockerfileOptions.setBaseImage(fromImage);

        handleProxyUrls();
        handleChown();
        handleAdditionalBuildCommands();

        if (buildArgs != null) {
            for (String arg : buildArgs.keySet()) {
                dockerfileOptions.buildArgs(arg);
            }
        }

        if (kubernetesTarget == KubernetesTarget.OPENSHIFT) {
            dockerfileOptions.useOwnerPermsForGroup(true);
            // if the user did not set the OS user:group, make the default oracle:root, instead of oracle:oracle
            if (!isChownSet()) {
                dockerfileOptions.setGroupId("root");
            }
        }

        logger.exiting();
    }

    /**
     * Set the docker options (dockerfile template bean) by extracting information from the fromImage.
     *
     * @throws IOException when a file operation fails.
     * @throws InterruptedException if an interrupt is received while trying to run a system command.
     */
    public void copyOptionsFromImage() throws IOException, InterruptedException {
        if (isOptionSet("--fromImage")) {
            logger.info("IMG-0002", fromImage);

            Properties baseImageProperties = Utils.getBaseImageProperties(buildEngine, fromImage,
                "/probe-env/inspect-image.sh", buildDir());

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

            // If the OS is busybox, the Dockerfile needs to know in order to use the correct security commands
            OperatingSystemProperties os = OperatingSystemProperties.getOperatingSystemProperties(baseImageProperties);
            // If OS is a BusyBox type OS, set usingBusyBox() to true, else set to false.
            if (os.name() != null) {
                // if OS name is in BUSYBOX_OS_NAMES, set usingBusybox to true
                dockerfileOptions.usingBusybox(BUSYBOX_OS_IDS.stream().anyMatch(os.id()::equalsIgnoreCase));
            }

            String pkgMgrProp = baseImageProperties.getProperty("packageManager", "YUM");

            PackageManagerType pkgMgr = PackageManagerType.valueOf(pkgMgrProp);
            logger.fine("fromImage package manager {0}", pkgMgr);
            if (pkgMgr == PackageManagerType.MICRODNF && os.version() != null && os.version().startsWith("8")) {
                logger.fine("Using older style format for microdnf for linux 8. ver={0}", os.version());
                pkgMgr = PackageManagerType.MICRODNF_8;
            }
            if (packageManager != PackageManagerType.OS_DEFAULT && pkgMgr != packageManager) {
                // If the user is overriding the detected package manager, use the provided value
                logger.info("IMG-0079", pkgMgr, packageManager);
                pkgMgr = packageManager;
            }
            dockerfileOptions.setPackageInstaller(pkgMgr);
        } else {
            dockerfileOptions.setPackageInstaller(packageManager);
        }
    }

    /**
     * Delete build context directory and remove all intermediate build images.
     *
     * @throws InterruptedException when interrupted.
     */
    public void cleanup() throws InterruptedException {
        if (skipcleanup) {
            return;
        }
        try {
            Utils.deleteFilesRecursively(buildDirectory);
        } catch (IOException e) {
            logger.severe("IMG-0080", buildDirectory);
        }

        if (!dryRun) {
            try {
                Utils.removeIntermediateDockerImages(buildEngine, buildId());
            } catch (IOException e) {
                logger.severe("IMG-0118", buildId());
            }
        }
    }

    /**
     * If the user provided a value to alter the default user:group, return true.
     *
     * @return true if the user provided a value to change user:group.
     */
    public boolean isChownSet() {
        return isOptionSet("--chown");
    }

    boolean isOptionSet(String optionName) {
        CommandLine.ParseResult pr = spec.commandLine().getParseResult();
        return pr.hasMatchedOption(optionName);
    }

    /**
     * Return successful build response and message.
     * @param startTime the time that the build started (for build duration).
     * @return exit response.
     */
    public CommandResponse successfulBuildResponse(Instant startTime) {
        Instant endTime = Instant.now();
        if (dryRun) {
            return CommandResponse.success("IMG-0054");
        } else {
            return CommandResponse.success("IMG-0053",
                Duration.between(startTime, endTime).getSeconds(), imageTag);
        }
    }

    public String imageTag() {
        return imageTag;
    }

    public String fromImage() {
        return fromImage;
    }

    public String buildId() {
        return buildId;
    }

    /**
     * Return build platforms from cli or settings or default system.
     * @return architecture list
     */
    public List<String> getBuildPlatform() {
        if (buildPlatform == null) {
            buildPlatform = new ArrayList<>();
            java.lang.String platform = ConfigManager.getInstance().getDefaultBuildPlatform();
            if (platform == null) {
                platform = Utils.standardPlatform(Architecture.getLocalArchitecture().toString());
            }
            buildPlatform.add(platform);
        }
        for (String platform : buildPlatform) {
            boolean valid = Architecture.AMD64.getAcceptableNames().contains(platform)
                || Architecture.ARM64.getAcceptableNames().contains(platform);
            if (!valid) {
                throw new IllegalArgumentException("Unknown build platform: " + platform);
            }
        }
        buildPlatform.replaceAll(value -> Utils.standardPlatform(Architecture.fromString(value).toString()));
        return buildPlatform.stream().distinct().collect(Collectors.toList());
    }

    @Option(
        names = {"--tag"},
        paramLabel = "<image tag>",
        required = true,
        description = "Tag for the final build image. Ex: container-registry.oracle.com/middleware/weblogic:12.2.1.4"
    )
    String imageTag;

    @Option(
        names = {"--fromImage"},
        paramLabel = FROM_IMAGE_LABEL,
        description = "Docker image to use as base image.  Default: ${DEFAULT-VALUE}",
        defaultValue = Constants.ORACLE_LINUX
    )
    private String fromImage;

    @Option(
        names = {"--httpProxyUrl"},
        paramLabel = "<HTTP proxy URL>",
        description = "proxy for http protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpProxyUrl;

    @Option(
        names = {"--httpsProxyUrl"},
        paramLabel = "<HTTPS proxy URL>",
        description = "proxy for https protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpsProxyUrl;

    @Option(
        names = {"--dockerLog"},
        description = "file to log output from the docker build.",
        hidden = true
    )
    private Path dockerLog;

    @Option(
        names = {"--skipcleanup"},
        description = "Do not delete the build context folder, intermediate images, and failed build containers."
    )
    boolean skipcleanup = false;

    @Option(
        names = {"--chown"},
        paramLabel = "<owner:group>",
        description = "owner and groupid to be used for files copied into the image. Default: ${DEFAULT-VALUE}",
        defaultValue = "oracle:oracle"
    )
    private String osUserAndGroup;

    @Option(
        names = {"--additionalBuildCommands"},
        paramLabel = "<filename>",
        description = "path to a file with additional build commands."
    )
    private Path additionalBuildCommandsPath;

    @Option(
        names = {"--additionalBuildFiles"},
        paramLabel = "<filename>",
        split = ",",
        description = "comma separated list of files that should be copied to the build context folder."
    )
    private List<Path> additionalBuildFiles;

    @Option(
        names = {"--dryRun"},
        description = "Skip image build execution and print Dockerfile to stdout."
    )
    boolean dryRun = false;

    @Option(
        names = {"--buildNetwork"},
        paramLabel = "<networking mode>",
        description = "Set the networking mode for the RUN instructions during build."
    )
    String buildNetwork;

    @Option(
        names = {"--pull"},
        description = "Always attempt to pull a newer version of base images during the build."
    )
    private boolean buildPull = false;

    @Option(
        names = {"--packageManager"},
        paramLabel = "<package manager>",
        description = "Override the detected package manager for installing OS packages."
    )
    PackageManagerType packageManager = PackageManagerType.OS_DEFAULT;

    @Option(
        names = {"--builder", "-b"},
        paramLabel = "<executable name>",
        description = "Executable to process the Dockerfile."
            + " Use the full path of the executable if not on your path."
            + " Defaults to 'docker', or, when set, to the value in environment variable WLSIMG_BUILDER."
    )
    String buildEngine = Constants.BUILDER_DEFAULT;

    @Option(
        names = {"--target"},
        paramLabel = "<target environment>",
        description = "Apply settings appropriate to the target environment.  Default: ${DEFAULT-VALUE}."
            + "  Supported values: ${COMPLETION-CANDIDATES}."
    )
    KubernetesTarget kubernetesTarget = KubernetesTarget.DEFAULT;

    @Option(
        names = {"--build-arg"},
        paramLabel = "<arg=value>",
        description = "Additional argument passed directly to the build engine."
    )
    Map<String,String> buildArgs;

    @Option(
        names = {"--platform"},
        paramLabel = "<target platform>",
        split = ",",
        description = "Set the target platform to build. Example: linux/amd64 or linux/arm64"
    )
    private List<String> buildPlatform;

    @Option(
        names = {"--push"},
        description = "push the image to the remote repository, only used when building multiplatform images"
    )
    private boolean push = false;

    @Option(
        names = {"--load"},
        description = "load the image to the local repository, only used when building multiplatform images"
    )
    private boolean load = false;

    @Option(
        names = {"--useBuildx"},
        description = "Use the BuildKit for building the container image (default when building multi-architecture)"
    )
    private boolean useBuildx;

    @Parameters(
        description = "Container build options.",
        hidden = true
    )
    List<String> buildOptions;


    @Spec
    CommandLine.Model.CommandSpec spec;

}
