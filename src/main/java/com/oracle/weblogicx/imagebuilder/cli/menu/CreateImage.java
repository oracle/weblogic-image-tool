/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.menu;

import com.oracle.weblogicx.imagebuilder.api.model.CachePolicy;
import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.impl.InstallerFile;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import com.oracle.weblogicx.imagebuilder.util.HttpUtil;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.oracle.weblogicx.imagebuilder.api.model.CachePolicy.ALWAYS;
import static com.oracle.weblogicx.imagebuilder.util.Constants.DEFAULT_JDK_VERSION;
import static com.oracle.weblogicx.imagebuilder.util.Constants.DEFAULT_WLS_VERSION;

@Command(
        name = "create",
        description = "Build WebLogic docker image",
        version = "1.0",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class CreateImage extends ImageOperation {

    private final Logger logger = Logger.getLogger(CreateImage.class.getName());

    public CreateImage() {
    }

    public CreateImage(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() throws Exception {

        Instant startTime = Instant.now();

        FileHandler fileHandler = setupLogger(isCLIMode);
        Path tmpDir = null;
        Path tmpDir2 = null;

        try {

            CommandResponse result = super.call();
            if (result.getStatus() != 0) {
                return result;
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            // create a tmp directory for user.
            tmpDir = Files.createTempDirectory(null);
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            logger.info("tmp directory used for build context: " + tmpDirPath);
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));

            // this handles wls, jdk, opatch_1394 and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            if (fromImage != null && !fromImage.isEmpty()) {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add("BASE_IMAGE=" + fromImage);

                tmpDir2 = Files.createTempDirectory(Paths.get(System.getProperty("user.home")), null);
                logger.info("tmp directory in user.home for docker run: " + tmpDir2);

                Utils.copyResourceAsFile("/probe-env/test-create-env.sh",
                        tmpDir2.toAbsolutePath().toString() + File.separator + "test-env.sh", true);

                List<String> imageEnvCmd = Utils.getDockerRunCmd(tmpDir2, fromImage, "test-env.sh");
                Properties baseImageProperties = Utils.runDockerCommand(imageEnvCmd);

                baseImageProperties.keySet().forEach(x -> logger.info(x + "=" + baseImageProperties.getProperty(x.toString())));

                boolean ohAlreadyExists = baseImageProperties.getProperty("WLS_VERSION", null) != null;

                if (ohAlreadyExists) {
                    return new CommandResponse(-1, "Oracle Home exists at location:" +
                            baseImageProperties.getProperty("ORACLE_HOME"));
                }

                String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
                if (!Utils.isEmptyString(pkgMgr)) {
                    filterStartTags.add(pkgMgr);
                }
            } else {
                filterStartTags.add("_YUM");
            }

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWDTArgsIfRequired(tmpDir));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            // Copy wls response file to tmpDir
            copyResponseFilesToDir(tmpDirPath);

            // Create Dockerfile
            Utils.replacePlaceHolders(tmpDirPath + File.separator + "Dockerfile", "/docker-files/Dockerfile.create", filterStartTags, "/docker-files/Dockerfile.ph");

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));
            Utils.runDockerCommand(cmdBuilder, dockerLog);

        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            Utils.deleteFilesRecursively(tmpDir);
            Utils.deleteFilesRecursively(tmpDir2);
            if (fileHandler != null) {
                fileHandler.close();
                logger.removeHandler(fileHandler);
            }
        }
        Instant endTime = Instant.now();
        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds() + "s. image tag: " + imageTag);
    }

    /**
     * Builds a list of build args to pass on to docker with the required installer files.
     * Also, creates links to installer files instead of copying over to build context dir.
     *
     * @param tmpDir build context directory
     * @return list of strings
     * @throws Exception in case of error
     */
    List<String> handleInstallerFiles(Path tmpDir) throws Exception {
        List<String> retVal = new LinkedList<>();
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        List<InstallerFile> requiredInstallers = gatherRequiredInstallers();
        for (InstallerFile eachInstaller : requiredInstallers) {
            String targetFilePath = eachInstaller.resolve(cacheStore);
            File targetFile = new File(targetFilePath);
            Path targetLink = Files.createLink(Paths.get(tmpDirPath, targetFile.getName()),
                    Paths.get(targetFilePath));
            retVal.addAll(eachInstaller.getBuildArg(tmpDir.relativize(targetLink).toString()));
        }
        return retVal;
    }

    @Override
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        if (Utils.compareVersions(installerVersion, DEFAULT_WLS_VERSION) == 0) {
            addOPatch1394ToImage(tmpDir);
        }
        //we need a local installerVersion variable for the command line Option. so propagate to super.
        super.installerVersion = installerVersion;
        return super.handlePatchFiles(tmpDir, tmpPatchesDir);
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
    private List<String> handleWDTArgsIfRequired(Path tmpDir) throws IOException {
        List<String> retVal = new LinkedList<>();
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        if (wdtModelPath != null) {
            if (Files.isRegularFile(wdtModelPath)) {
                filterStartTags.add("WDT_");
                Path targetLink = Files.createLink(Paths.get(tmpDirPath, wdtModelPath.getFileName().toString()), wdtModelPath);
                retVal.add(Constants.BUILD_ARG);
                retVal.add("WDT_MODEL=" + tmpDir.relativize(targetLink).toString());

                if (wdtArchivePath != null && Files.isRegularFile(wdtArchivePath)) {
                    targetLink = Files.createLink(Paths.get(tmpDirPath, wdtArchivePath.getFileName().toString()), wdtArchivePath);
                    retVal.add(Constants.BUILD_ARG);
                    retVal.add("WDT_ARCHIVE=" + tmpDir.relativize(targetLink).toString());
                }

                if (wdtVariablesPath != null && Files.isRegularFile(wdtVariablesPath)) {
                    targetLink = Files.createLink(Paths.get(tmpDirPath, wdtVariablesPath.getFileName().toString()), wdtVariablesPath);
                    retVal.add(Constants.BUILD_ARG);
                    retVal.add("WDT_VARIABLE=" + tmpDir.relativize(targetLink).toString());
                    retVal.addAll(getWDTRequiredBuildArgs(wdtVariablesPath));
                }

                Path tmpScriptsDir = Files.createDirectory(Paths.get(tmpDirPath, "scripts"));
                String toScriptsPath = tmpScriptsDir.toAbsolutePath().toString();
                Utils.copyResourceAsFile("/container-scripts/startAdminServer.sh", toScriptsPath, true);
                Utils.copyResourceAsFile("/container-scripts/startManagedServer.sh", toScriptsPath, true);
                Utils.copyResourceAsFile("/container-scripts/waitForAdminServer.sh", toScriptsPath, true);
            } else {
                throw new IOException("WDT model file " + wdtModelPath + " not found");
            }
        }
        return retVal;
    }

    /**
     * Certain environment variables need to be set in docker images for WDT domains to work.
     *
     * @param wdtVariablesPath wdt variables file path.
     * @return list of build args
     * @throws IOException in case of error
     */
    private List<String> getWDTRequiredBuildArgs(Path wdtVariablesPath) throws IOException {
        List<String> retVal = new LinkedList<>();
        Properties variableProps = new Properties();
        variableProps.load(new FileInputStream(wdtVariablesPath.toFile()));
        List<Object> matchingKeys = variableProps.keySet().stream().filter(
                x -> variableProps.getProperty(((String) x)) != null &&
                        Constants.REQD_WDT_BUILD_ARGS.contains(((String) x).toUpperCase())
        ).collect(Collectors.toList());
        matchingKeys.forEach(x -> {
            retVal.add(Constants.BUILD_ARG);
            retVal.add(((String) x).toUpperCase() + "=" + variableProps.getProperty((String) x));
        });
        return retVal;
    }

    /**
     * Builds a list of {@link InstallerFile} objects based on user input which are processed
     * to download the required install artifacts
     *
     * @return list of InstallerFile
     * @throws Exception in case of error
     */
    private List<InstallerFile> gatherRequiredInstallers() throws Exception {
        List<InstallerFile> retVal = new LinkedList<>();
        retVal.add(new InstallerFile(InstallerType.fromValue(installerType.toString()), installerVersion,
                (useCache != ALWAYS), userId, password));
        retVal.add(new InstallerFile(InstallerType.JDK, jdkVersion, (useCache != ALWAYS), userId, password));
        if (wdtModelPath != null && Files.isRegularFile(wdtModelPath)) {
            InstallerFile wdtInstaller = new InstallerFile(InstallerType.WDT, wdtVersion, (useCache != ALWAYS),
                    null, null);
            retVal.add(wdtInstaller);
            addWDTURL(wdtInstaller.getKey() + "_url");
        }
        return retVal;
    }

    /**
     * Parses wdtVersion and constructs the url to download WDT and adds the url to cache
     *
     * @param wdtURLKey key in the format wdt_0.17
     * @throws Exception in case of error
     */
    private void addWDTURL(String wdtURLKey) throws Exception {
        if ("latest".equalsIgnoreCase(wdtVersion) || cacheStore.getValueFromCache(wdtURLKey) == null) {
            List<String> wdtTags = HttpUtil.getWDTTags();
            String tagToMatch = "latest".equalsIgnoreCase(wdtVersion) ? wdtTags.get(0) : "weblogic-deploy-tooling-" + wdtVersion;
            if (wdtTags.contains(tagToMatch)) {
                String downloadLink = String.format(Constants.WDT_URL_FORMAT, tagToMatch);
                logger.info("WDT Download link = " + downloadLink);
                cacheStore.addToCache(wdtURLKey, downloadLink);
            } else {
                throw new Exception("Couldn't find WDT download url for version:" + wdtVersion);
            }
        }
    }

    /**
     * Copies response files required for wls install to the tmp directory which provides docker build context
     *
     * @param dirPath directory to copy to
     * @throws IOException in case of error
     */
    private void copyResponseFilesToDir(String dirPath) throws IOException {
        Utils.copyResourceAsFile("/response-files/wls.rsp", dirPath, false);
        Utils.copyResourceAsFile("/response-files/oraInst.loc", dirPath, false);
    }

    @Option(
            names = {"--type"},
            description = "Installer type. default: ${DEFAULT-VALUE}. supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private WLSInstallerType installerType;

    @Option(
            names = {"--version"},
            description = "Installer version. default: ${DEFAULT-VALUE}",
            required = true,
            defaultValue = DEFAULT_WLS_VERSION
    )
    private String installerVersion;

    @Option(
            names = {"--jdkVersion"},
            description = "Version of server jdk to install. default: ${DEFAULT-VALUE}",
            required = true,
            defaultValue = DEFAULT_JDK_VERSION
    )
    private String jdkVersion;

    @Option(
            names = {"--useCache"},
            paramLabel = "<Cache Policy>",
            defaultValue = "always",
            description = "Whether to use local cache or download installers.\n" +
                    "first - try to use cache and download artifacts if required\n" +
                    "always - default. use cache always and never download artifacts\n" +
                    "never - never use cache and always download artifacts",
            hidden = true
    )
    private CachePolicy useCache;

    @Option(
            names = {"--fromImage"},
            description = "Docker image to use as base image."
    )
    private String fromImage;

    @Option(
            names = {"--wdtModel"},
            description = "path to the wdt model file to create domain with"
    )
    private Path wdtModelPath;

    @Option(
            names = {"--wdtArchive"},
            description = "path to wdt archive file used by wdt model"
    )
    private Path wdtArchivePath;

    @Option(
            names = {"--wdtVariables"},
            description = "path to wdt variables file used by wdt model"
    )
    private Path wdtVariablesPath;

    @Option(
            names = {"--wdtVersion"},
            description = "wdt version to create the domain",
            defaultValue = "latest"
    )
    private String wdtVersion;
}
