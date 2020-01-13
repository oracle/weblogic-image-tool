// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.api.model.FmwInstallerType;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import com.oracle.weblogic.imagetool.cachestore.CachedPatchFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.ValidationResult;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;


public abstract class CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonOptions.class);
    protected CacheStore cacheStore = new CacheStoreFactory().get();
    DockerfileOptions dockerfileOptions;
    private String tempDirectory = null;
    private String nonProxyHosts = null;

    abstract String getInstallerVersion();

    private void handleChown() {
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

    private void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, Constants.HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, Constants.HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    String getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            tempDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", tempDirectory);
        }
        return tempDirectory;
    }

    void init(String buildId) throws Exception {
        logger.entering(buildId);
        dockerfileOptions = new DockerfileOptions(buildId);
        logger.info("IMG-0016", buildId);

        handleProxyUrls();
        password = Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
        // check user support credentials if useCache not set to always and we are applying any patches

        if (userId != null || password != null) {
            if (!ARUUtil.checkCredentials(userId, password)) {
                throw new Exception("user Oracle support credentials do not match");
            }
        }

        handleChown();
        handleAdditionalBuildCommands();

        logger.exiting();
    }

    /**
     * Returns true if any patches should be applied.
     * A PSU is considered a patch.
     * @return true if applying patches
     */
    boolean applyingPatches() {
        return latestPSU || !patches.isEmpty();
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     *
     * @return list of strings
     * @throws Exception in case of error
     */
    List<String> handlePatchFiles(String previousInventory) throws Exception {
        logger.entering();
        List<String> retVal = new LinkedList<>();

        if (!applyingPatches()) {
            return retVal;
        }

        String toPatchesPath = createPatchesTempDirectory().toAbsolutePath().toString();

        List<String> patchLocations = new LinkedList<>();

        List<String> patchList = new ArrayList<>(patches);

        if (latestPSU) {
            if (userId == null || password == null) {
                throw new Exception(Utils.getMessage("IMG-0031"));
            }

            // PSUs for WLS and JRF installers are considered WLS patches
            String patchId = ARUUtil.getLatestPSUNumber(FmwInstallerType.WLS, getInstallerVersion(), userId, password);

            if (Utils.isEmptyString(patchId)) {
                latestPSU = false;
                logger.fine("Latest PSU NOT FOUND, ignoring latestPSU flag");
            } else {
                logger.fine("Found latest PSU {0}", patchId);
                CachedFile psu = new CachedPatchFile(getInstallerVersion(), patchId, userId, password);
                patchLocations.add(psu.resolve(cacheStore));
                // Add PSU patch ID to the patchList for validation (conflict check)
                patchList.add(patchId);
            }
        }

        logger.info("IMG-0012");
        ValidationResult validationResult = ARUUtil.validatePatches(previousInventory, patchList, userId, password);
        if (validationResult.isSuccess()) {
            logger.info("IMG-0006");
        } else {
            String error = validationResult.getErrorMessage();
            logger.severe(error);
            throw new IllegalArgumentException(error);
        }

        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                patchLocations.add(
                    new CachedPatchFile(getInstallerVersion(), patchId, userId, password).resolve(cacheStore)
                );
            }
        }
        for (String patchLocation : patchLocations) {
            if (patchLocation != null) {
                File patchFile = new File(patchLocation);
                Files.copy(Paths.get(patchLocation), Paths.get(toPatchesPath, patchFile.getName()));
            } else {
                logger.severe("IMG-0024");
            }
        }
        if (!patchLocations.isEmpty()) {
            dockerfileOptions.setPatchingEnabled();
        }
        logger.exiting(retVal.size());
        return retVal;
    }

    private Path createPatchesTempDirectory() throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(tempDirectory, "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }


    void installOpatchInstaller(String tmpDir, String opatchBugNumber) throws Exception {
        // opatch patch now is in the format #####_opatch in the cache store
        // So the version passing to the constructor of CachedPatchFile is also "opatch".
        // since opatch releases is on it's own and there is not really a patch to opatch
        // and the version is embedded in the zip file version.txt

        String filePath =
            new CachedPatchFile(Constants.OPATCH_PATCH_TYPE, opatchBugNumber, userId, password).resolve(cacheStore);
        String filename = new File(filePath).getName();
        Files.copy(Paths.get(filePath), Paths.get(tmpDir, filename));
        dockerfileOptions.setOPatchPatchingEnabled();
        dockerfileOptions.setOPatchFileName(filename);
    }


    String getUserId() {
        return userId;
    }

    String getPassword() {
        return password;
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
    private String userId;

    private String password;

    @Option(
        names = {"--password"},
        interactive = true,
        arity = "0..1",
        paramLabel = "<support password>",
        description = "Enter password for Oracle Support userId on STDIN"
    )
    private String passwordStr;

    @Option(
        names = {"--passwordEnv"},
        paramLabel = "<environment variable>",
        description = "environment variable containing the support password"
    )
    private String passwordEnv;

    @Option(
        names = {"--passwordFile"},
        paramLabel = "<password file>",
        description = "path to file containing just the password"
    )
    private Path passwordFile;

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
    private Path dockerLog;

    @Option(
        names = {"--skipcleanup"},
        description = "Do no delete Docker context folder or intermediate images.",
        hidden = true
    )
    boolean skipcleanup = false;

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
