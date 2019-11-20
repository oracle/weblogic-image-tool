// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.impl.meta.CacheStoreFactory;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;


public class CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonOptions.class);
    protected CacheStore cacheStore = new CacheStoreFactory().get();
    DockerfileOptions dockerfileOptions;
    private String tempDirectory = null;
    private String nonProxyHosts = null;

    public void handleChown() {
        if (osUserAndGroup.length != 2) {
            throw new IllegalArgumentException("--chown value must be a colon separated user and group.  user:group");
        }

        Pattern p = Pattern.compile("^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$");
        Matcher usr = p.matcher(osUserAndGroup[0]);
        if (!usr.matches()) {
            throw new IllegalArgumentException("--chown must contain a valid Unix username.  No more than 32 characters"
                + " and starts with a lowercase character.  Invalid value = " + osUserAndGroup[0]);
        }
        Matcher grp = p.matcher(osUserAndGroup[1]);
        if (!grp.matches()) {
            throw new IllegalArgumentException("--chown must contain a valid Unix groupid.  No more than 32 characters"
                + " and starts with a lowercase character.  Invalid value = " + osUserAndGroup[1]);
        }

        dockerfileOptions.setUserId(osUserAndGroup[0]);
        dockerfileOptions.setGroupId(osUserAndGroup[1]);
    }

    public void handleAdditionalBuildCommands() throws IOException {
        if (additionalBuildCommandsPath != null) {
            if (!Files.isRegularFile(additionalBuildCommandsPath)) {
                throw new FileNotFoundException("Additional build command file does not exist: "
                    + additionalBuildCommandsPath);
            }

            AdditionalBuildCommands additionalBuildCommands = AdditionalBuildCommands.load(additionalBuildCommandsPath);
            dockerfileOptions.setAdditionalBuildCommands(additionalBuildCommands.getContents());
        }
    }

    void runDockerCommand(String dockerfile, List<String> command) throws IOException, InterruptedException {
        logger.info("docker cmd = " + String.join(" ", command));

        if (dryRun) {
            System.out.println("########## BEGIN DOCKERFILE ##########");
            System.out.println(dockerfile);
            System.out.println("########## END DOCKERFILE ##########");
        } else {
            Utils.runDockerCommand(command, dockerLog);
        }
    }


    /**
     * Builds the options for docker build command.
     *
     * @return list of options
     */
    List<String> getInitialBuildCmd() {

        logger.entering();
        List<String> cmdBuilder = Stream.of("docker", "build",
            "--force-rm=true", "--no-cache").collect(Collectors.toList());

        cmdBuilder.add("--tag");
        cmdBuilder.add(imageTag);

        if (!Utils.isEmptyString(httpProxyUrl)) {
            cmdBuilder.add(Constants.BUILD_ARG);
            cmdBuilder.add("http_proxy=" + httpProxyUrl);
        }

        if (!Utils.isEmptyString(httpsProxyUrl)) {
            cmdBuilder.add(Constants.BUILD_ARG);
            cmdBuilder.add("https_proxy=" + httpsProxyUrl);
        }

        if (!Utils.isEmptyString(nonProxyHosts)) {
            cmdBuilder.add(Constants.BUILD_ARG);
            cmdBuilder.add("no_proxy=" + nonProxyHosts);
        }

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.set(0, dockerPath.toAbsolutePath().toString());
        }
        logger.exiting();
        return cmdBuilder;
    }

    public void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, Constants.HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, Constants.HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    public String getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            tempDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", tempDirectory);
        }
        return tempDirectory;
    }

    public String init(String buildId) throws Exception {
        logger.finer("Entering ImageOperation call ");
        dockerfileOptions = new DockerfileOptions(buildId);
        logger.info("IMG-0016", buildId);

        handleProxyUrls();
        String password = handlePasswordOptions();
        // check user support credentials if useCache not set to always and we are applying any patches

        if (userId != null || password != null) {
            if (!ARUUtil.checkCredentials(userId, password)) {
                throw new Exception("user Oracle support credentials do not match");
            }
        }

        handleChown();
        handleAdditionalBuildCommands();

        logger.finer("Exiting ImageOperation call ");
        return password;
    }

    /**
     * Determines the support password by parsing the possible three input options.
     *
     * @return String form of password
     * @throws IOException in case of error
     */
    public String handlePasswordOptions() throws IOException {
        return Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
    }

    @Option(
        names = {"--tag"},
        paramLabel = "TAG",
        required = true,
        description = "Tag for the final build image. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    String imageTag;

    @Option(
        names = {"--user"},
        paramLabel = "<support email>",
        description = "Oracle Support email id"
    )
    String userId;

    @Option(
        names = {"--password"},
        paramLabel = "<password for support user id>",
        description = "Password for support userId"
    )
    String passwordStr;

    @Option(
        names = {"--passwordEnv"},
        paramLabel = "<environment variable>",
        description = "environment variable containing the support password"
    )
    String passwordEnv;

    @Option(
        names = {"--passwordFile"},
        paramLabel = "<password file>",
        description = "path to file containing just the password"
    )
    Path passwordFile;

    @Option(
        names = {"--httpProxyUrl"},
        description = "proxy for http protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    String httpProxyUrl;

    @Option(
        names = {"--httpsProxyUrl"},
        description = "proxy for https protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    String httpsProxyUrl;

    @Option(
        names = {"--docker"},
        description = "path to docker executable. Default: ${DEFAULT-VALUE}",
        defaultValue = "docker"
    )
    private Path dockerPath;

    @Option(
        names = {"--dockerLog"},
        description = "file to log output from the docker build",
        hidden = true
    )
    Path dockerLog;


    @Option(
        names = {"--cleanup"},
        description = "Cleanup temporary files. Default: ${DEFAULT-VALUE}.",
        defaultValue = "true",
        hidden = true
    )
    boolean cleanup;

    @Option(
        names = {"--chown"},
        split = ":",
        description = "userid:groupid for JDK/Middleware installs and patches. Default: ${DEFAULT-VALUE}.",
        defaultValue = "oracle:oracle"
    )
    private String[] osUserAndGroup;

    @Option(
        names = {"--additionalBuildCommands"},
        description = "path to a file with additional build commands"
    )
    private Path additionalBuildCommandsPath;

    @Option(
        names = {"--dryRun"},
        description = "Skip Docker build execution and print Dockerfile to stdout"
    )
    boolean dryRun = false;

    @Option(
        names = {"--latestPSU"},
        description = "Whether to apply patches from latest PSU."
    )
    boolean latestPSU = false;

    @Option(
        names = {"--patches"},
        paramLabel = "patchId",
        split = ",",
        description = "Comma separated patch Ids. Ex: 12345678,87654321"
    )
    List<String> patches = new ArrayList<>();


    @Option(
        names = {"--opatchBugNumber"},
        description = "the patch number for OPatch (patching OPatch)"
    )
    String opatchBugNumber = "28186730";

    @Unmatched
    List<String> unmatchedOptions;
}
