/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli.menu;

import com.oracle.weblogicx.imagebuilder.builder.api.FileResolver;
import com.oracle.weblogicx.imagebuilder.builder.api.model.CachePolicy;
import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSVersionValues;
import com.oracle.weblogicx.imagebuilder.builder.impl.InstallerFile;
import com.oracle.weblogicx.imagebuilder.builder.impl.PatchFile;
import com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.HttpUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.builder.api.model.CachePolicy.ALWAYS;
import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.BUILD_ARG;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.CLI_OPTION;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.DEFAULT_WLS_VERSION;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.OPATCH_1394_KEY;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.REQD_WDT_BUILD_ARGS;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.WDT_URL_FORMAT;

@Command(
        name = "create",
        //mixinStandardHelpOptions = true,
        description = "Build WebLogic docker image",
        version = "1.0",
        sortOptions = false,
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class CreateImage implements Callable<CommandResponse> {

    private Logger logger = Logger.getLogger("com.oracle.weblogix.imagebuilder.builder");

    @Override
    public CommandResponse call() throws Exception {

        Instant startTime = Instant.now();

        System.out.println("hello");
        System.out.println("WLSInstallerType = \"" + installerType + "\"");
        System.out.println("InstallerVersion = \"" + installerVersion + "\"");
        System.out.println("latestPSU = \"" + latestPSU + "\"");
        System.out.println("patches = \"" + patches + "\"");
        System.out.println("fromImage = \"" + fromImage + "\"");
        System.out.println("userId = \"" + userId + "\"");
        System.out.println("password = \"" + password + "\"");

        FileHandler fileHandler = setupLogger(unmatcheOptions.contains(CLI_OPTION));
        Path tmpDir = null;

        try {

            Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl);

            // check credentials if useCache option allows us to download artifacts
            if (useCache != ALWAYS) {
                if (!ARUUtil.checkCredentials(userId, password)) {
                    return new CommandResponse(-1, "User credentials do not match");
                }
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            // create a tmp directory for user.
            tmpDir = Files.createTempDirectory(null);
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            System.out.println("tmpDir = " + tmpDirPath);
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();

            // build wdt args if user passes --wdtModelPath
            cmdBuilder.addAll(handleWDTArgsIfRequired(tmpDir));

            // this handles wls, jdk, opatch_1394 and wdt install files.
            cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            // Copy Dockerfile.create to tmpDir
            copyResponseFilesToDir(tmpDirPath);

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));

            // process builder
            ProcessBuilder processBuilder = new ProcessBuilder(cmdBuilder);
            final Process process = processBuilder.start();

            Path dockerLogPath = Utils.createFile(dockerLog, "dockerbuild.log");
            System.out.println("dockerLog: " + dockerLog);
            if (dockerLogPath != null) {
                try (
                        BufferedReader processReader = new BufferedReader(new InputStreamReader(
                                process.getInputStream()));
                        PrintWriter logWriter = new PrintWriter(new FileWriter(dockerLogPath.toFile()))
                ) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        logWriter.println(line);
                        System.out.println(line);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (process.waitFor() != 0) {
                try (BufferedReader stderr =
                             new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    String line;
                    while ((line = stderr.readLine()) != null) {
                        stringBuilder2.append(line);
                    }
                    throw new IOException(
                            "docker command failed with error: " + stringBuilder2.toString());
                }
            }

        } catch (Exception ex) {
            //ex.printStackTrace();
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            if (tmpDir != null) {
                Files.walk(tmpDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        //.peek(System.out::println)
                        .forEach(File::delete);
            }
            if (fileHandler != null) {
                fileHandler.close();
                logger.removeHandler(fileHandler);
            }
        }
        Instant endTime = Instant.now();
        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds()  + "s. image tag: " + imageTag);
    }

    /**
     * Builds a list of build args to pass on to docker with the required installer files.
     * Also, creates links to installer files instead of copying over to build context dir.
     * @param tmpDir build context directory
     * @return list of strings
     * @throws Exception in case of error
     */
    private List<String> handleInstallerFiles(Path tmpDir) throws Exception {
        List<String> retVal = new LinkedList<>();
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        List<InstallerFile> requiredInstallers = gatherRequiredInstallers();
        for (InstallerFile eachInstaller : requiredInstallers) {
            String targetFilePath = eachInstaller.resolve(META_RESOLVER);
            File targetFile = new File(targetFilePath);
            Path targetLink = Files.createLink(Paths.get(tmpDirPath, targetFile.getName()),
                    Paths.get(targetFilePath));
            retVal.addAll(eachInstaller.getBuildArg(tmpDir.relativize(targetLink).toString()));
        }
        return retVal;
    }

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     * @param tmpDir build context dir
     * @param tmpPatchesDir patches dir under build context
     * @return list of strings
     * @throws Exception in case of error
     */
    private List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        List<String> retVal = new LinkedList<>();
        List<String> patchLocations = new LinkedList<>();
        String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();
        if (latestPSU) {
            System.out.println("Getting latest PSU");
            FileResolver psuResolver = new PatchFile(installerType, installerVersion, null, userId, password);
            patchLocations.add(psuResolver.resolve(META_RESOLVER));
        }
        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                patchLocations.add(new PatchFile(installerType, installerVersion, patchId, userId, password)
                        .resolve(META_RESOLVER));
            }
        }
        for (String patchLocation : patchLocations) {
            if (patchLocation != null) {
                File patch_file = new File(patchLocation);
                //System.out.println(patchLocation + "? exists: " + patch_file.exists() + ", filename: " +
                // patch_file.getName());
                Files.createLink(Paths.get(toPatchesPath, patch_file.getName()), Paths.get(patchLocation));
            } else {
                logger.severe("null entry in patchLocations");
            }
        }
        if (!patchLocations.isEmpty()) {
            retVal.add(BUILD_ARG);
            retVal.add("PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());
        }
        return retVal;
    }

    /**
     * Checks whether the user requested a domain to be created with WDT.
     * If so, returns the required build args to pass to docker and creates required file links to pass
     * the model, archive, variables file to build process
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @return list of build args
     * @throws IOException in case of error
     */
    private List<String> handleWDTArgsIfRequired(Path tmpDir) throws IOException {
        List<String> retVal = new LinkedList<>();
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        if (wdtModelPath != null) {
            if (Files.isRegularFile(wdtModelPath)) {
                // bake in wdt
                Utils.replacePlaceHolders(tmpDirPath + File.separator + "Dockerfile", "/Dockerfile.create", "/Dockerfile.wdt");
                Path targetLink = Files.createLink(Paths.get(tmpDirPath, wdtModelPath.getFileName().toString()), wdtModelPath);
                retVal.add(BUILD_ARG);
                retVal.add("WDT_MODEL=" + tmpDir.relativize(targetLink).toString());

                if (wdtArchivePath != null && Files.isRegularFile(wdtArchivePath)) {
                    targetLink = Files.createLink(Paths.get(tmpDirPath, wdtArchivePath.getFileName().toString()), wdtArchivePath);
                    retVal.add(BUILD_ARG);
                    retVal.add("WDT_ARCHIVE=" + tmpDir.relativize(targetLink).toString());
                }

                if (wdtVariablesPath != null && Files.isRegularFile(wdtVariablesPath)) {
                    targetLink = Files.createLink(Paths.get(tmpDirPath, wdtVariablesPath.getFileName().toString()), wdtVariablesPath);
                    retVal.add(BUILD_ARG);
                    retVal.add("WDT_VARIABLE=" + tmpDir.relativize(targetLink).toString());
                    retVal.addAll(getWDTRequiredBuildArgs(wdtVariablesPath));
                }

                Path tmpScriptsDir = Files.createDirectory(Paths.get(tmpDirPath, "scripts"));
                String toScriptsPath = tmpScriptsDir.toAbsolutePath().toString();
                Utils.copyResourceAsFile("/scripts/startAdminServer.sh", toScriptsPath);
                Utils.copyResourceAsFile("/scripts/startManagedServer.sh", toScriptsPath);
                Utils.copyResourceAsFile("/scripts/waitForAdminServer.sh", toScriptsPath);
            } else {
                throw new IOException("WDT model file " + wdtModelPath + " not found");
            }
        } else {
            Utils.copyResourceAsFile("/Dockerfile.create", tmpDirPath + File.separator + "Dockerfile");
        }
        return retVal;
    }

    /**
     * Certain environment variables need to be set in docker images for WDT domains to work.
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
                        REQD_WDT_BUILD_ARGS.contains(((String) x).toUpperCase())
        ).collect(Collectors.toList());
        matchingKeys.forEach( x -> {
            retVal.add(BUILD_ARG);
            retVal.add(((String) x).toUpperCase() + "=" + variableProps.getProperty((String) x));
        });
        return retVal;
    }

    /**
     * Builds a list of {@link InstallerFile} objects based on user input which are processed
     * to download the required install artifacts
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
        if (DEFAULT_WLS_VERSION.equals(installerVersion) ) {
            retVal.add(new InstallerFile(OPATCH_1394_KEY, (useCache != ALWAYS), userId, password));
        }
        return retVal;
    }

    /**
     * Parses wdtVersion and constructs the url to download WDT and adds the url to cache
     * @param wdtURLKey key in the format wdt_0.17
     * @throws Exception in case of error
     */
    private void addWDTURL(String wdtURLKey) throws Exception {
        if ("latest".equalsIgnoreCase(wdtVersion) || META_RESOLVER.getValueFromCache(wdtURLKey) == null) {
            List<String> wdtTags = HttpUtil.getWDTTags();
            String tagToMatch = "latest".equalsIgnoreCase(wdtVersion) ? wdtTags.get(0) : "weblogic-deploy-tooling-" + wdtVersion;
            if (wdtTags.contains(tagToMatch)) {
                String downloadLink = String.format(WDT_URL_FORMAT, tagToMatch);
                System.out.println("WDT Download link = " + downloadLink);
                META_RESOLVER.addToCache(wdtURLKey, downloadLink);
            } else {
                throw new Exception("Couldn't find WDT download url for version:" + wdtVersion);
            }
        }
    }

    /**
     * Copies response files required for wls install to the tmp directory which provides docker build context
     * @param dirPath directory to copy to
     * @throws IOException in case of error
     */
    private void copyResponseFilesToDir(String dirPath) throws IOException {
        Utils.copyResourceAsFile("/wls.rsp", dirPath);
        Utils.copyResourceAsFile("/oraInst.loc", dirPath);
    }

    /**
     * Enable logging when using cli mode and required log file path is supplied
     * @param isCLIMode whether tool is run in cli mode
     * @return log file handler or null
     */
    private FileHandler setupLogger(boolean isCLIMode) {
        FileHandler fileHandler = null;
        try {
            if (isCLIMode) {
                Path toolLogPath = Utils.createFile(toolLog, "tool.log");
                System.out.println("toolLogPath: " + toolLogPath);
                if (toolLogPath != null) {
                    fileHandler = new FileHandler(toolLogPath.toAbsolutePath().toString());
                    fileHandler.setFormatter(new SimpleFormatter());
                    logger.addHandler(fileHandler);
                    logger.setLevel(Level.INFO);
                }
            } else {
                logger.setLevel(Level.OFF);
            }
        } catch (IOException e) {
            //suppress exception
            fileHandler = null;
        }
        return fileHandler;
    }

    /**
     * Builds the options of the docker command
     * @return list of options for docker build command
     */
    private List<String> getInitialBuildCmd() {

        List<String> cmdBuilder = Stream.of("docker", "build",
                "--squash", "--force-rm", "--no-cache", "--network=host").collect(Collectors.toList());

        cmdBuilder.add("--tag");
        cmdBuilder.add(imageTag);

        if (httpProxyUrl != null && !httpProxyUrl.isEmpty()) {
            cmdBuilder.add(BUILD_ARG);
            cmdBuilder.add("http_proxy=" + httpProxyUrl);
        } else {
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add(BUILD_ARG);
                cmdBuilder.add(String.format("http_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }

        if (httpsProxyUrl != null && !httpsProxyUrl.isEmpty()) {
            cmdBuilder.add(BUILD_ARG);
            cmdBuilder.add("https_proxy=" + httpsProxyUrl);
        } else {
            String proxyHost = System.getProperty("https.proxyHost");
            String proxyPort = System.getProperty("https.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add(BUILD_ARG);
                cmdBuilder.add(String.format("https_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.set(0, dockerPath.toAbsolutePath().toString());
        }
        return cmdBuilder;
    }


    @Option(
            names = { "--installerType" },
            description = "Installer type. Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private WLSInstallerType installerType;

    @Option(
            names = { "--installerVersion" },
            description = "Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = DEFAULT_WLS_VERSION,
            completionCandidates = WLSVersionValues.class
    )
    private String installerVersion;

    @Option(
            names = { "--jdkVersion" },
            description = "Ex: 8u201",
            //description = "Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "8u201"
            //completionCandidates = JDKVersionValues.class
    )
    private String jdkVersion;

    @Option(
            names = { "--latestPSU" },
            description = "Whether to apply patches from latest PSU."
    )
    private boolean latestPSU = false;

    @Option(
            names = { "--patches" },
            paramLabel = "patchId",
            split = ",",
            description = "Comma separated patch Ids. Ex: p12345678,p87654321"
    )
    private List<String> patches;

    @Option(
            names = { "--fromImage" },
            description = "Docker image to use as base image.",
            hidden = true
    )
    private String fromImage;

    @Option(
            names = { "--tag" },
            paramLabel = "TAG",
            required = true,
            description = "Tag for the final build image. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    private String imageTag;

    @Option(
            names = { "--user" },
            paramLabel = "<support email>",
            description = "Oracle Support email id"
    )
    private String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<password for support user id>",
            description = "Password for support userId"
    )
    private String password;

    @Option(
            names = { "--useCache" },
            paramLabel = "<Cache Policy>",
            defaultValue = "always",
            description = "Whether to use local cache or download artifacts.\n" +
                    "first - try to use cache and download artifacts if required\n" +
                    "always - default. use cache always and never download artifacts\n" +
                    "never - never use cache and always download artifacts",
            hidden = true
    )
    private CachePolicy useCache;

//    @Option(
//            hidden = true,
//            names = { "--publish" },
//            description = "Publish this docker image"
//    )
//    private boolean isPublish = false;

    @Option(
        names = { "--httpProxyUrl" },
        description = "proxy for http protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpProxyUrl;

    @Option(
            names = { "--httpsProxyUrl" },
            description = "proxy for https protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    private String httpsProxyUrl;

    @Option(
            names = { "--toolLog" },
            description = "file to log output from this tool. This is different from the docker build log.",
            hidden = true
    )
    private Path toolLog;

    @Option(
            names = { "--dockerLog" },
            description = "file to log output from the docker build",
            hidden = true
    )
    private Path dockerLog;

    @Option(
            names = {"--docker"},
            description = "path to docker executable. Default: ${DEFAULT-VALUE}",
            defaultValue = "docker"
    )
    private Path dockerPath;

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

    @Unmatched
    List<String> unmatcheOptions;
}
