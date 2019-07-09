// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.FileResolver;
import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.PatchFile;
import com.oracle.weblogic.imagetool.impl.meta.CacheStoreFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

public abstract class ImageOperation implements Callable<CommandResponse> {

    private final Logger logger = Logger.getLogger(ImageOperation.class.getName());
    // DockerfileOptions provides switches and values to the customize the Dockerfile template
    protected DockerfileOptions dockerfileOptions;
    protected CacheStore cacheStore = new CacheStoreFactory().get();
    private String nonProxyHosts = null;
    boolean isCLIMode;
    String password;

    ImageOperation() {
        dockerfileOptions = new DockerfileOptions();
    }

    ImageOperation(boolean isCLIMode) {
        this();
        this.isCLIMode = isCLIMode;
    }

    @Override
    public CommandResponse call() throws Exception {
        logger.finer("Entering ImageOperation call ");
        handleProxyUrls();
        password = handlePasswordOptions();
        // check user support credentials if useCache not set to always and we are applying any patches

        if (userId != null || password != null) {
            if (!ARUUtil.checkCredentials(userId, password)) {
                return new CommandResponse(-1, "user Oracle support credentials do not match");
            }
        }

        handleChown();

        logger.finer("Exiting ImageOperation call ");
        return new CommandResponse(0, null);
    }

    /**
     * Determines the support password by parsing the possible three input options.
     *
     * @return String form of password
     * @throws IOException in case of error
     */
    private String handlePasswordOptions() throws IOException {
        return Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     *
     * @param tmpDir        build context dir
     * @param tmpPatchesDir patches dir under build context
     * @return list of strings
     * @throws Exception in case of error
     */
    List<String> handlePatchFiles(String tmpDir, Path tmpPatchesDir) throws Exception {
        logger.finer("Entering ImageOperation.handlePatchFiles");
        List<String> retVal = new LinkedList<>();
        List<String> patchLocations = new LinkedList<>();
        String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();

        if (latestPSU) {
            if (userId == null || password == null) {
                throw new Exception("No credentials provided. Cannot determine "
                        + "latestPSU");
            } else {
                String patchId = ARUUtil.getLatestPSUNumber(installerType.toString(), installerVersion,
                        userId,
                        password);
                if (Utils.isEmptyString(patchId)) {
                    throw new Exception(String.format("Failed to find latest psu for product category %s, version %s",
                            installerType.toString(), installerVersion));
                }
                logger.finest("Found latest PSU " + patchId);
                FileResolver psuResolver = new PatchFile(useCache, installerType.toString(), installerVersion,
                        patchId, userId, password);
                patchLocations.add(psuResolver.resolve(cacheStore));
            }

        }
        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                patchLocations.add(new PatchFile(useCache, installerType.toString(), installerVersion,
                        patchId, userId, password).resolve(cacheStore));
            }
        }
        for (String patchLocation : patchLocations) {
            if (patchLocation != null) {
                File patchFile = new File(patchLocation);
                Files.copy(Paths.get(patchLocation), Paths.get(toPatchesPath, patchFile.getName()));
            } else {
                logger.severe("null entry in patchLocation");
            }
        }
        if (!patchLocations.isEmpty()) {
            retVal.add(Constants.BUILD_ARG);
            retVal.add("PATCHDIR=" + Paths.get(tmpDir).relativize(tmpPatchesDir).toString());
            dockerfileOptions.setPatchingEnabled();
        }
        logger.finer("Exiting ImageOperation.handlePatchFiles");
        return retVal;
    }

    /**
     * Builds the options for docker build command.
     *
     * @return list of options
     */
    List<String> getInitialBuildCmd() {

        logger.finer("Entering ImageOperation.getInitialBuildCmd");
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
        logger.finer("Exiting ImageOperation.getInitialBuildCmd");
        return cmdBuilder;
    }

    public String getTempDirectory() throws IOException {
        Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
        String pathAsString = tmpDir.toAbsolutePath().toString();
        logger.info("tmp directory used for docker build context: " + pathAsString);
        return pathAsString;
    }

    public Path createPatchesTempDirectory(String tmpDir) throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDir, "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }

    void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, Constants.HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, Constants.HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    void addOPatch1394ToImage(String tmpDir, String opatchBugNumber) throws Exception {
        // opatch patch now is in the format #####_opatch in the cache store
        // So the version passing to the constructor of PatchFile is also "opatch".
        // since opatch releases is on it's own and there is not really a patch to opatch
        // and the version is embedded in the zip file version.txt

        String filePath =
                new PatchFile(useCache, Constants.OPATCH_PATCH_TYPE, Constants.OPATCH_PATCH_TYPE, opatchBugNumber,
                        userId, password).resolve(cacheStore);
        Files.copy(Paths.get(filePath), Paths.get(tmpDir, new File(filePath).getName()));
        dockerfileOptions.setOPatchPatchingEnabled();
    }

    private void handleChown() {
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


    WLSInstallerType installerType = WLSInstallerType.WLS;

    String installerVersion = Constants.DEFAULT_WLS_VERSION;

    @Option(
            names = {"--useCache"},
            paramLabel = "<Cache Policy>",
            defaultValue = "always",
            hidden = true,
            description = "this should not be used"
    )
    CachePolicy useCache;

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

    @Unmatched
    List<String> unmatchedOptions;
}
