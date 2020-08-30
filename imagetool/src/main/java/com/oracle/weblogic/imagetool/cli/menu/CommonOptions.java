// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.cachestore.OPatchFile;
import com.oracle.weblogic.imagetool.cachestore.PatchFile;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.AdditionalBuildCommands;
import com.oracle.weblogic.imagetool.util.AruUtil;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerBuildCommand;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import com.oracle.weblogic.imagetool.util.VerrazzanoModel;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

public abstract class CommonOptions {
    private static final LoggingFacade logger = LoggingFactory.getLogger(CommonOptions.class);
    DockerfileOptions dockerfileOptions;
    private String tempDirectory = null;
    private String nonProxyHosts = null;

    private List<Object> resolveOptions = null;
    private List<Path> resolveFiles = null;

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

        cmdBuilder.forceRm(!skipcleanup)
            .tag(imageTag)
            .network(buildNetwork)
            .pull(buildPull)
            .buildArg("http_proxy", httpProxyUrl)
            .buildArg("https_proxy", httpsProxyUrl)
            .buildArg("no_proxy", nonProxyHosts);

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.dockerPath(dockerPath.toAbsolutePath().toString());
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

    /**
     * Override the default behavior for generating a temp directory.
     * This method is used by UNIT tests ONLY.
     * @param value should be the value of a temp directory generated by the the UNIT test framework
     */
    void setTempDirectory(String value) {
        tempDirectory = value;
    }

    void init(String buildId) throws Exception {
        logger.entering(buildId);
        dockerfileOptions = new DockerfileOptions(buildId);
        logger.info("IMG-0016", buildId);

        handleProxyUrls();
        password = Utils.getPasswordFromInputs(passwordStr, passwordFile, passwordEnv);
        // check user support credentials if useCache not set to always and we are applying any patches

        if (userId != null || password != null) {
            if (!AruUtil.checkCredentials(userId, password)) {
                throw new Exception("user Oracle support credentials do not match");
            }
        }

        handleChown();
        handleAdditionalBuildCommands();

        logger.exiting();
    }

    List<Path> gatherFiles() {
        if (resolveFiles == null) {
            resolveFiles = new ArrayList<>();
        }
        if (verrazzanoModel != null) {
            resolveFiles.add(verrazzanoModel);
        }
        return resolveFiles;
    }

    List<Object> resolveOptions() {
        if (resolveFiles != null && !resolveFiles.isEmpty()) {
            resolveOptions = Collections.singletonList(new VerrazzanoModel(imageTag, dockerfileOptions.domain_home()));
        }
        return resolveOptions;
    }

    /**
     * Returns true if any patches should be applied.
     * A PSU is considered a patch.
     *
     * @return true if applying patches
     */
    boolean applyingPatches() {
        return (latestPSU || recommendedPatches) || !patches.isEmpty();
    }

    /**
     * Should OPatch version be updated.
     * OPatch should be updated to the latest version available unless the user
     * requests that OPatch should not be updated.
     * @return true if OPatch should be updated.
     */
    boolean shouldUpdateOpatch() {
        if (skipOpatchUpdate) {
            logger.fine("IMG-0065");
        }
        return !skipOpatchUpdate;
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     *
     * @throws Exception in case of error
     */
    void handlePatchFiles(FmwInstallerType installerType) throws Exception {
        handlePatchFiles(installerType, null, null);
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     *
     * @param previousInventory existing inventory found in the "from" image
     * @throws Exception in case of error
     */
    void handlePatchFiles(FmwInstallerType installerType, String previousInventory, String psuVersion)
        throws Exception {
        logger.entering(psuVersion);
        if (!applyingPatches()) {
            return;
        }

        String toPatchesPath = createPatchesTempDirectory().toAbsolutePath().toString();

        List<PatchFile> patchFiles = new ArrayList<>();
        if (recommendedPatches || latestPSU) {
            if (userId == null || password == null) {
                throw new Exception(Utils.getMessage("IMG-0031"));
            }
        }
        if (recommendedPatches) {
            // Get the latest PSU and its recommended patches
            List<String> patchList =
                AruUtil.getRecommendedPatches(installerType, getInstallerVersion(), userId, password);

            if (patchList.isEmpty()) {
                recommendedPatches = false;
                logger.fine("Latest PSU and recommended patches NOT FOUND, ignoring recommendedPatches flag");
            } else {
                for (String patchId: patchList) {
                    if (FmwInstallerType.getWeblogicServerTypes().contains(installerType)
                        && "31544353".equals(patchId)) {
                        logger.fine("Skipping ADR patch {0} for WLS installer: {1}", patchId, installerType);
                    } else {
                        logger.fine("Add latest recommended patch {0} to list", patchId);
                        patchFiles.add(new PatchFile(patchId, getInstallerVersion(), psuVersion, userId, password));
                    }
                }
            }
        } else if (latestPSU) {
            // PSUs for WLS and JRF installers are considered WLS patches
            List<String> patchIds = AruUtil.getLatestPsuNumber(installerType, getInstallerVersion(), userId, password);

            if (patchIds.isEmpty()) {
                latestPSU = false;
                logger.fine("Latest PSU NOT FOUND, ignoring latestPSU flag");
            } else {
                logger.fine("Found latest PSU {0}", patchIds);
                for (String patchId : patchIds) {
                    patchFiles.add(new PatchFile(patchId, getInstallerVersion(), null, userId, password));
                }
            }
        }

        // add user-provided patch list to full patch list to be applied
        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                // if user mistakenly added the OPatch patch to the WLS patch list, skip it
                if (!OPatchFile.DEFAULT_BUG_NUM.equals(patchId)) {
                    patchFiles.add(new PatchFile(patchId, getInstallerVersion(), psuVersion, userId, password));
                }
            }
        }

        AruUtil.validatePatches(previousInventory, patchFiles, userId, password);

        for (PatchFile patch : patchFiles) {
            String patchLocation = patch.resolve(cache());
            if (patchLocation != null && !Utils.isEmptyString(patchLocation)) {
                File patchFile = new File(patchLocation);
                try {
                    Files.copy(Paths.get(patchLocation), Paths.get(toPatchesPath, patchFile.getName()));
                } catch (FileAlreadyExistsException ee) {
                    logger.warning("IMG-0077", patch.getKey());
                }
            } else {
                logger.severe("IMG-0024", patch.getKey());
            }
        }
        if (!patchFiles.isEmpty()) {
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
        String filePath = new OPatchFile(opatchBugNumber, userId, password, cache())
            .resolve(cache());
        String filename = new File(filePath).getName();
        Files.copy(Paths.get(filePath), Paths.get(tmpDir, filename));
        dockerfileOptions.setOPatchPatchingEnabled();
        dockerfileOptions.setOPatchFileName(filename);
    }

    /**
     * Set the docker options for build if fromImage parameter is present.
     * @param fromImage  image tag
     * @param tmpDir temporary directory
     * @throws Exception thrown by getBaseImageProperties
     */
    public void copyOptionsFromImage(String fromImage, String tmpDir)
        throws Exception {

        if (fromImage != null && !fromImage.isEmpty()) {
            logger.finer("IMG-0002", fromImage);
            dockerfileOptions.setBaseImage(fromImage);

            Utils.copyResourceAsFile("/probe-env/test-create-env.sh",
                tmpDir + File.separator + "test-env.sh", true);

            Properties baseImageProperties = Utils.getBaseImageProperties(fromImage, tmpDir);

            if (baseImageProperties.getProperty("WLS_VERSION", null) != null) {
                throw new IllegalArgumentException(Utils.getMessage("IMG-0038", fromImage,
                    baseImageProperties.getProperty("ORACLE_HOME")));
            }

            String existingJavaHome = baseImageProperties.getProperty("JAVA_HOME", null);
            if (existingJavaHome != null) {
                dockerfileOptions.disableJavaInstall(existingJavaHome);
                logger.info("IMG-0000", existingJavaHome);
            }

            String pkgMgrProp = baseImageProperties.getProperty("PACKAGE_MANAGER", "YUM");

            PackageManagerType pkgMgr = PackageManagerType.valueOf(pkgMgrProp);
            logger.fine("fromImage package manager {0}", pkgMgr);
            if (packageManager != PackageManagerType.OS_DEFAULT && pkgMgr != packageManager) {
                logger.info("IMG-0079", pkgMgr, packageManager);
                pkgMgr = packageManager;
            }
            dockerfileOptions.setPackageInstaller(pkgMgr);
        } else if (packageManager == PackageManagerType.OS_DEFAULT) {
            // Default OS is Oracle Linux 7-slim, so default package manager is YUM
            dockerfileOptions.setPackageInstaller(PackageManagerType.YUM);
        } else {
            dockerfileOptions.setPackageInstaller(packageManager);
        }
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
        description = "Do no delete Docker context folder, intermediate images, and failed build container."
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
        names = {"--recommendedPatches"},
        description = "Whether to apply recommended patches from latest PSU."
    )
    boolean recommendedPatches = false;

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
    String opatchBugNumber;

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
        names = {"--skipOpatchUpdate"},
        description = "Do not update OPatch version, even if a newer version is available."
    )
    private boolean skipOpatchUpdate = false;

    @Option(
        names = {"--vzModel"},
        description = "For verrazzano, resolve parameters in the verrazzano model with information from the image tool."
    )
    Path verrazzanoModel;

    @Option(
        names = {"--packageManager"},
        description = "Set the Linux package manager to use for installing OS packages. Default: ${DEFAULT-VALUE}"
    )
    PackageManagerType packageManager = PackageManagerType.OS_DEFAULT;

    @SuppressWarnings("unused")
    @Unmatched
    List<String> unmatchedOptions;
}
