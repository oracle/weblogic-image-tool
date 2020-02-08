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

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import com.oracle.weblogic.imagetool.cachestore.CachedPatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerBuildCommand;
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

        if (additionalBuildFiles != null) {
            final String FILES_DIR = "files";
            Files.createDirectory(Paths.get(getTempDirectory(), FILES_DIR));
            for (Path additionalFile : additionalBuildFiles) {
                if (!Files.isReadable(additionalFile)) {
                    throw new FileNotFoundException(Utils.getMessage("IMG-0030", additionalFile));
                }
                Path targetFile = Paths.get(getTempDirectory(), FILES_DIR, additionalFile.getFileName().toString());
                logger.info("IMG-0043", additionalFile);
                if (Files.isDirectory(additionalFile)) {
                    Utils.copyLocalDirectory(additionalFile, targetFile, false);
                } else {
                    Utils.copyLocalFile(additionalFile, targetFile, false);
                }
            }
        }
    }

    void runDockerCommand(String dockerfile, DockerBuildCommand command) throws IOException, InterruptedException {
        logger.info("docker cmd = " + command.toString());

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
    DockerBuildCommand getInitialBuildCmd(String contextFolder) {

        logger.entering();
        DockerBuildCommand cmdBuilder = new DockerBuildCommand(contextFolder);

        cmdBuilder.setTag(imageTag);

        if (!Utils.isEmptyString(httpProxyUrl)) {
            cmdBuilder.addBuildArg("http_proxy", httpProxyUrl);
        }

        if (!Utils.isEmptyString(httpsProxyUrl)) {
            cmdBuilder.addBuildArg("https_proxy", httpsProxyUrl);
        }

        if (!Utils.isEmptyString(nonProxyHosts)) {
            cmdBuilder.addBuildArg("no_proxy", nonProxyHosts);
        }

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.setDockerPath(dockerPath.toAbsolutePath().toString());
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
     * @param previousInventory existing inventory found in the "from" image
     * @throws Exception in case of error
     */
    void handlePatchFiles(String previousInventory) throws Exception {
        logger.entering();
        if (!applyingPatches()) {
            return;
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
        logger.exiting();
    }

    private Path createPatchesTempDirectory() throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(getTempDirectory(), "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }


    void installOpatchInstaller(String tmpDir, String opatchBugNumber) throws Exception {
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
        names = {"--additionalBuildFiles"},
        split = ",",
        description = "comma separated list of files that should be copied to the build context folder"
    )
    private List<Path> additionalBuildFiles;

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
    String opatchBugNumber = "28186730_13.9.4.2.2";

    @Unmatched
    List<String> unmatchedOptions;
}
