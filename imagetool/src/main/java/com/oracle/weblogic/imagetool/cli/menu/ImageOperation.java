// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.FileResolver;
import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.DomainType;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.InstallerFile;
import com.oracle.weblogic.imagetool.impl.PatchFile;
import com.oracle.weblogic.imagetool.impl.meta.CacheStoreFactory;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.ARUUtil;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.ValidationResult;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

public abstract class ImageOperation implements Callable<CommandResponse> {

    private static final LoggingFacade logger = LoggingFactory.getLogger(ImageOperation.class);
    // DockerfileOptions provides switches and values to the customize the Dockerfile template
    DockerfileOptions dockerfileOptions;
    protected CacheStore cacheStore = new CacheStoreFactory().get();
    private String nonProxyHosts = null;
    private String tempDirectory = null;
    String password;
    String buildId;

    //ImageOperation() {
    //}

    @Override
    public CommandResponse call() throws Exception {
        logger.finer("Entering ImageOperation call ");

        buildId = UUID.randomUUID().toString();
        dockerfileOptions = new DockerfileOptions(buildId);
        logger.info("IMG-0016", buildId);

        handleProxyUrls();
        password = handlePasswordOptions();
        // check user support credentials if useCache not set to always and we are applying any patches

        if (userId != null || password != null) {
            if (!ARUUtil.checkCredentials(userId, password)) {
                return new CommandResponse(-1, "user Oracle support credentials do not match");
            }
        }

        handleChown();
        handleAdditionalBuildCommands();

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
     * Return the WLS installer type for this operation.
     * @return WLS, FMW, or RestrictedJRF
     */
    public abstract WLSInstallerType getInstallerType();

    /**
     * Return the WLS installer version string.
     * @return something like 12.2.1.3.0
     */
    public abstract String getInstallerVersion();

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
                throw new Exception("No credentials provided. Cannot determine latestPSU");
            }

            // PSUs for WLS and JRF installers are considered WLS patches
            String patchId = ARUUtil.getLatestPSUNumber(WLSInstallerType.WLS, getInstallerVersion(), userId, password);
            if (Utils.isEmptyString(patchId)) {
                throw new Exception(String.format("Failed to find latest psu for product category %s, version %s",
                    getInstallerType(), getInstallerVersion()));
            }
            logger.fine("Found latest PSU {0}", patchId);
            FileResolver psuResolver = new PatchFile(useCache, getInstallerType().toString(), getInstallerVersion(),
                patchId, userId, password);
            patchLocations.add(psuResolver.resolve(cacheStore));
            // Add PSU patch ID to the patchList for validation (conflict check)
            patchList.add(patchId);
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
                patchLocations.add(new PatchFile(useCache, getInstallerType().toString(), getInstallerVersion(),
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
            dockerfileOptions.setPatchingEnabled();
        }
        logger.exiting(retVal.size());
        return retVal;
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

    public String getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            Path tmpDir = Files.createTempDirectory(Paths.get(Utils.getBuildWorkingDir()), "wlsimgbuilder_temp");
            tempDirectory = tmpDir.toAbsolutePath().toString();
            logger.info("IMG-0003", tempDirectory);
        }
        return tempDirectory;
    }

    public Path createPatchesTempDirectory() throws IOException {
        Path tmpPatchesDir = Files.createDirectory(Paths.get(getTempDirectory(), "patches"));
        Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));
        return tmpPatchesDir;
    }

    private void handleProxyUrls() throws IOException {
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

    private void handleAdditionalBuildCommands() throws IOException {
        if (additionalBuildCommandsPath != null) {
            if (!Files.isRegularFile(additionalBuildCommandsPath)) {
                throw new FileNotFoundException("Additional build command file does not exist: "
                    + additionalBuildCommandsPath);
            }

            AdditionalBuildCommands additionalBuildCommands = AdditionalBuildCommands.load(additionalBuildCommandsPath);
            dockerfileOptions.setAdditionalBuildCommands(additionalBuildCommands.getContents());
        }
    }

    /**
     * Builds a list of build args to pass on to docker with the required installer files.
     * And, copies the installers over to build context dir.
     *
     * @param tmpDir build context directory
     * @return list of build argument parameters for docker build
     * @throws Exception in case of error
     */
    List<String> handleInstallerFiles(String tmpDir) throws Exception {

        logger.entering(tmpDir);
        List<String> retVal = new LinkedList<>();
        List<InstallerFile> requiredInstallers = gatherRequiredInstallers();
        for (InstallerFile installerFile : requiredInstallers) {
            String targetFilePath = installerFile.resolve(cacheStore);
            logger.finer("copying targetFilePath: {0}", targetFilePath);
            String filename = new File(targetFilePath).getName();
            try {
                Files.copy(Paths.get(targetFilePath), Paths.get(tmpDir, filename));
                retVal.addAll(installerFile.getBuildArg(filename));
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        logger.exiting(retVal);
        return retVal;
    }

    /**
     * Builds a list of {@link InstallerFile} objects based on user input which are processed.
     * to download the required install artifacts
     *
     * @return list of InstallerFile
     * @throws Exception in case of error
     */
    protected List<InstallerFile> gatherRequiredInstallers() throws Exception {
        logger.entering();
        List<InstallerFile> result = new LinkedList<>();
        if (wdtModelPath != null) {
            logger.finer("IMG-0001", InstallerType.WDT, wdtVersion);
            InstallerFile wdtInstaller = new InstallerFile(useCache, InstallerType.WDT, wdtVersion, null, null);
            result.add(wdtInstaller);
            addWdtUrl(wdtInstaller.getKey());
        }
        logger.exiting(result.size());
        return result;
    }

    /**
     * Checks whether the user requested a domain to be created with WDT.
     * If so, returns the required build args to pass to docker and creates required file links to pass
     * the model, archive, variables file to build process
     *
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @return list of build args
     * @throws IOException in case of error
     */
    List<String> handleWdtArgsIfRequired(String tmpDir) throws IOException {
        logger.entering(tmpDir);

        List<String> retVal = new LinkedList<>();
        if (wdtModelPath != null) {
            dockerfileOptions.setWdtEnabled();
            dockerfileOptions.setWdtModelOnly(wdtModelOnly);
            String[] modelFiles = wdtModelPath.toString().split(",");
            List<String> modelList = new ArrayList<>();

            for (String modelFile : modelFiles) {
                Path modelFilePath = Paths.get(modelFile);
                if (Files.isRegularFile(modelFilePath)) {
                    String modelFilename = modelFilePath.getFileName().toString();
                    Files.copy(modelFilePath, Paths.get(tmpDir, modelFilename));
                    modelList.add(modelFilename);
                } else {
                    throw new IOException("WDT model file " + modelFile + " not found");
                }
            }
            dockerfileOptions.setWdtModels(modelList);

            dockerfileOptions.setWdtDomainType(wdtDomainType);
            if (wdtDomainType != DomainType.WLS) {
                if (getInstallerType() != WLSInstallerType.FMW) {
                    throw new IOException("FMW installer is required for JRF domain");
                }
                dockerfileOptions.setRunRcu(runRcu);
            }

            if (wdtArchivePath != null && Files.isRegularFile(wdtArchivePath)) {
                String wdtArchiveFilename = wdtArchivePath.getFileName().toString();
                Files.copy(wdtArchivePath, Paths.get(tmpDir, wdtArchiveFilename));
                //Until WDT supports multiple archives, take single file argument from CLI and convert to list
                dockerfileOptions.setWdtArchives(Collections.singletonList(wdtArchiveFilename));
            }
            dockerfileOptions.setDomainHome(wdtDomainHome);

            dockerfileOptions.setJavaOptions(wdtJavaOptions);

            if (wdtVariablesPath != null && Files.isRegularFile(wdtVariablesPath)) {
                String wdtVariableFilename = wdtVariablesPath.getFileName().toString();
                Files.copy(wdtVariablesPath, Paths.get(tmpDir, wdtVariableFilename));
                //Until WDT supports multiple variable files, take single file argument from CLI and convert to list
                dockerfileOptions.setWdtVariables(Collections.singletonList(wdtVariableFilename));
            }

            dockerfileOptions.setWdtStrictValidation(wdtStrictValidation);
        }
        logger.exiting();
        return retVal;
    }

    /**
     * Parses wdtVersion and constructs the url to download WDT and adds the url to cache.
     *
     * @param wdtKey key in the format wdt_0.17
     * @throws Exception in case of error
     */
    private void addWdtUrl(String wdtKey) throws Exception {
        logger.entering(wdtKey);
        String wdtUrlKey = wdtKey + "_url";
        if (cacheStore.getValueFromCache(wdtKey) == null) {
            if (userId == null || password == null) {
                throw new Exception("CachePolicy prohibits download. Add the required wdt installer to cache");
            }
            List<String> wdtTags = HttpUtil.getWDTTags();
            String tagToMatch = "latest".equalsIgnoreCase(wdtVersion) ? wdtTags.get(0) :
                    "weblogic-deploy-tooling-" + wdtVersion;
            if (wdtTags.contains(tagToMatch)) {
                String downloadLink = String.format(Constants.WDT_URL_FORMAT, tagToMatch);
                logger.info("IMG-0007", downloadLink);
                cacheStore.addToCache(wdtUrlKey, downloadLink);
            } else {
                throw new Exception("Couldn't find WDT download url for version:" + wdtVersion);
            }
        }
        logger.exiting();
    }

    public DomainType getWdtDomainType() {
        return wdtDomainType;
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


    @Option(
        names = {"--opatchBugNumber"},
        description = "the patch number for OPatch (patching OPatch)"
    )
    String opatchBugNumber = "28186730";

    @Option(
            names = {"--wdtModel"},
            description = "path to the WDT model file that defines the Domain to create"
    )
    private Path wdtModelPath;

    @Option(
            names = {"--wdtArchive"},
            description = "path to the WDT archive file used by the WDT model"
    )
    private Path wdtArchivePath;

    @Option(
            names = {"--wdtVariables"},
            description = "path to the WDT variables file for use with the WDT model"
    )
    private Path wdtVariablesPath;

    @Option(
            names = {"--wdtVersion"},
            description = "WDT tool version to use",
            defaultValue = "latest"
    )
    private String wdtVersion;

    @Option(
            names = {"--wdtDomainType"},
            description = "WDT Domain Type. Default: ${DEFAULT-VALUE}. Supported values: ${COMPLETION-CANDIDATES}"
    )
    private DomainType wdtDomainType = DomainType.WLS;

    @Option(
            names = "--wdtRunRCU",
            description = "instruct WDT to run RCU when creating the Domain"
    )
    private boolean runRcu = false;

    @Option(
            names = {"--wdtDomainHome"},
            description = "pass to the -domain_home for wdt",
            defaultValue = "/u01/domains/base_domain"
    )
    private String  wdtDomainHome;

    @Option(
            names = {"--wdtJavaOptions"},
            description = "Java command line options for WDT"
    )
    private String wdtJavaOptions;

    @Option(
        names = {"--wdtModelOnly"},
        description = "Install WDT and copy the models to the image, but do not create the domain. "
            + "Default: ${DEFAULT-VALUE}."
    )
    private boolean wdtModelOnly = false;

    @Option(
        names = {"--wdtStrictValidation"},
        description = "Use strict validation for the WDT validation method. Only applies when using model only.  "
            + "Default: ${DEFAULT-VALUE}."
    )
    private boolean wdtStrictValidation = false;

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

    @Unmatched
    List<String> unmatchedOptions;
}
