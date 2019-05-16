/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.cli.menu;

import com.oracle.weblogic.imagetool.api.model.CachePolicy;
import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.api.model.DomainType;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.impl.InstallerFile;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Command(
        name = "create",
        description = "Build WebLogic docker image",
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
            // using user home as base to avoid running out of space in tmp
            tmpDir = Files.createTempDirectory(Paths.get(System.getProperty("user.home")),
                "wlsimgbuilder_temp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            logger.info("tmp directory used for build context: " + tmpDirPath);
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));

            // this handles wls, jdk and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            if (fromImage != null && !fromImage.isEmpty()) {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add("BASE_IMAGE=" + fromImage);

                tmpDir2 = Files.createTempDirectory(Paths.get(System.getProperty("user.home")),
                    "wlsimgbuilder_temp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
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

                if (useCache != CachePolicy.ALWAYS) {
                    String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
                    if (!Utils.isEmptyString(pkgMgr)) {
                        filterStartTags.add(pkgMgr);
                    }
                }
            } else {
                if (useCache != CachePolicy.ALWAYS) {
                    filterStartTags.add("_YUM");
                }
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
            Utils.runDockerCommand(isCLIMode, cmdBuilder, dockerLog);
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
    private List<String> handleInstallerFiles(Path tmpDir) throws Exception {
        List<String> retVal = new LinkedList<>();
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        List<InstallerFile> requiredInstallers = gatherRequiredInstallers();
        for (InstallerFile eachInstaller : requiredInstallers) {
            String targetFilePath = eachInstaller.resolve(cacheStore);
            File targetFile = new File(targetFilePath);
            try {
                Path targetLink = Files.copy(Paths.get(targetFilePath), Paths.get(tmpDirPath, targetFile.getName()));
                retVal.addAll(eachInstaller.getBuildArg(tmpDir.relativize(targetLink).toString()));
            } catch (Exception ee ) {
                ee.printStackTrace();
            }
        }
        return retVal;
    }

    @Override
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        if ((latestPSU || !patches.isEmpty()) && Utils.compareVersions(installerVersion, Constants.DEFAULT_WLS_VERSION) == 0) {
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
                if (domainType != DomainType.WLS) {
                    if (installerType != WLSInstallerType.FMW) {
                        throw new IOException("FMW installer is required for JRF domain");
                    }
                    retVal.add(Constants.BUILD_ARG);
                    retVal.add("DOMAIN_TYPE=" + domainType);
                    if (rcu_run_flag) {
                        retVal.add(Constants.BUILD_ARG);
                        retVal.add("RCU_RUN_FLAG=" + "-run_rcu");
                    }
                }
                filterStartTags.add("WDT_");
                Path targetLink = Files.copy(wdtModelPath, Paths.get(tmpDirPath, wdtModelPath.getFileName().toString())
                    );
                retVal.add(Constants.BUILD_ARG);
                retVal.add("WDT_MODEL=" + tmpDir.relativize(targetLink).toString());

                if (wdtArchivePath != null && Files.isRegularFile(wdtArchivePath)) {
                    targetLink = Files.copy(wdtArchivePath, Paths.get(tmpDirPath,
                        wdtArchivePath.getFileName().toString())
                        );
                    retVal.add(Constants.BUILD_ARG);
                    retVal.add("WDT_ARCHIVE=" + tmpDir.relativize(targetLink).toString());
                }

                if (wdtVariablesPath != null && Files.isRegularFile(wdtVariablesPath)) {
                    targetLink = Files.copy(wdtVariablesPath, Paths.get(tmpDirPath,
                        wdtVariablesPath.getFileName().toString())
                        );
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
        if (wdtModelPath != null && Files.isRegularFile(wdtModelPath)) {
            InstallerFile wdtInstaller = new InstallerFile(useCache, InstallerType.WDT, wdtVersion, null, null);
            retVal.add(wdtInstaller);
            addWDTURL(wdtInstaller.getKey());
        }
        retVal.add(new InstallerFile(useCache, InstallerType.fromValue(installerType.toString()), installerVersion,
                userId, password));
        retVal.add(new InstallerFile(useCache, InstallerType.JDK, jdkVersion, userId, password));
        return retVal;
    }

    /**
     * Parses wdtVersion and constructs the url to download WDT and adds the url to cache
     *
     * @param wdtKey key in the format wdt_0.17
     * @throws Exception in case of error
     */
    private void addWDTURL(String wdtKey) throws Exception {
        String wdtURLKey = wdtKey + "_url";
        if (cacheStore.getValueFromCache(wdtKey) == null) {
            if (useCache == CachePolicy.ALWAYS) {
                throw new Exception("CachePolicy prohibits download. Add the required wdt installer to cache");
            }
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
            defaultValue = Constants.DEFAULT_WLS_VERSION
    )
    private String installerVersion;

    @Option(
            names = {"--jdkVersion"},
            description = "Version of server jdk to install. default: ${DEFAULT-VALUE}",
            required = true,
            defaultValue = Constants.DEFAULT_JDK_VERSION
    )
    private String jdkVersion;

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

    @Option(
            names = {"--domainType"},
            description = "type of domain to create. default: ${DEFAULT-VALUE}. supported values: ${COMPLETION-CANDIDATES}",
            defaultValue = "wls",
            required = true
    )
    private DomainType domainType;

    @Option(
            names = "--run_rcu",
            description = "whether to run rcu to create the required database schemas"
    )
    private boolean rcu_run_flag = false;
}
