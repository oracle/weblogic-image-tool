// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.aru.InvalidCredentialException;
import com.oracle.weblogic.imagetool.builder.BuildCommand;
import com.oracle.weblogic.imagetool.cli.HelpVersionProvider;
import com.oracle.weblogic.imagetool.inspect.OperatingSystemProperties;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.InvalidPatchIdFormatException;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import static com.oracle.weblogic.imagetool.util.Constants.BUSYBOX;

public abstract class CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonOptions.class);
    private static final String FILESFOLDER = "files";

    DockerfileOptions dockerfileOptions;
    private String buildDirectory = null;
    private String nonProxyHosts = null;
    private String buildId;

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

    private void handleAdditionalBuildCommands() throws IOException {
        if (additionalBuildCommandsPath != null) {
            if (!Files.isRegularFile(additionalBuildCommandsPath)) {
                throw new FileNotFoundException(Utils.getMessage("IMG-0030", additionalBuildCommandsPath));
            }

            AdditionalBuildCommands additionalBuildCommands = AdditionalBuildCommands.load(additionalBuildCommandsPath);
            dockerfileOptions.setAdditionalBuildCommands(additionalBuildCommands.getContents());
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

    void runDockerCommand(String dockerfile, BuildCommand command) throws IOException, InterruptedException {
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
        return buildDirectory;
    }

    /**
     * Override the default behavior for generating a temp directory.
     * This method is used by UNIT tests ONLY.
     * @param value should be the value of a temp directory generated by the the UNIT test framework
     */
    void setBuildDirectory(String value) {
        buildDirectory = value;
    }

    void initializeOptions() throws InvalidCredentialException, IOException, InvalidPatchIdFormatException {
        logger.entering();
        buildId = UUID.randomUUID().toString();
        logger.info(HelpVersionProvider.versionString());
        logger.info("IMG-0016", buildId);
        dockerfileOptions = new DockerfileOptions(buildId);

        handleProxyUrls();
        handleChown();
        handleAdditionalBuildCommands();

        if (kubernetesTarget == KubernetesTarget.OpenShift) {
            dockerfileOptions.setDomainGroupAsUser(true);
            // if the user did not set the OS user:group, make the default oracle:root, instead of oracle:oracle
            if (osUserAndGroup == null) {
                dockerfileOptions.setGroupId("root");
            }
        }

        logger.exiting();
    }

    /**
     * Set the docker options (dockerfile template bean) by extracting information from the fromImage.
     * @param fromImage image tag of the starting image
     * @param tmpDir    name of the temp directory to use for the build context
     * @throws IOException when a file operation fails.
     * @throws InterruptedException if an interrupt is received while trying to run a system command.
     */
    public void copyOptionsFromImage(String fromImage, String tmpDir) throws IOException, InterruptedException {

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

            OperatingSystemProperties os = OperatingSystemProperties.getOperatingSystemProperties(baseImageProperties);
            if (os.name() != null && os.name().equalsIgnoreCase(BUSYBOX)) {
                dockerfileOptions.usingBusybox(true);
            }

            String pkgMgrProp = baseImageProperties.getProperty("packageManager", "YUM");

            PackageManagerType pkgMgr = PackageManagerType.valueOf(pkgMgrProp);
            logger.fine("fromImage package manager {0}", pkgMgr);
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

    public String imageTag() {
        return imageTag;
    }

    public String fromImage() {
        return fromImage;
    }

    public String buildId() {
        return buildId;
    }

    @Option(
        names = {"--tag"},
        paramLabel = "TAG",
        required = true,
        description = "Tag for the final build image. Ex: container-registry.oracle.com/middleware/weblogic:12.2.1.4"
    )
    String imageTag;

    @Option(
        names = {"--fromImage"},
        description = "Docker image to use as base image."
    )
    private String fromImage;

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
        split = ":",
        description = "userid:groupid for JDK/Middleware installs and patches. Default: oracle:oracle."
    )
    private String[] osUserAndGroup;

    @Option(
        names = {"--additionalBuildCommands"},
        description = "path to a file with additional build commands."
    )
    private Path additionalBuildCommandsPath;

    @Option(
        names = {"--additionalBuildFiles"},
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
        description = "Override the detected Linux package manager for installing OS packages."
    )
    PackageManagerType packageManager = PackageManagerType.OS_DEFAULT;

    @Option(
        names = {"--builder", "-b"},
        description = "Executable to process the Dockerfile. Default: ${DEFAULT-VALUE}."
    )
    String buildEngine = "docker";

    @Option(
        names = {"--target"},
        description = "Apply settings appropriate to the target environment.  Default: ${DEFAULT-VALUE}."
            + "  Supported values: ${COMPLETION-CANDIDATES}."
    )
    KubernetesTarget kubernetesTarget = KubernetesTarget.Default;

    @SuppressWarnings("unused")
    @Unmatched

    List<String> unmatchedOptions;
}
