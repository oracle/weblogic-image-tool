/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli.menu;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "update",
        //mixinStandardHelpOptions = true,
        description = "Update WebLogic docker image with selected patches",
        version = "1.0",
        sortOptions = false,
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage implements Callable<CommandResponse> {
    @Override
    public CommandResponse call() throws Exception {
        return null;
    }


    /*
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
        System.out.println("publish = \"" + isPublish + "\"");

        FileHandler fileHandler = setupLogger(unmatcheOptions.contains(CLI_OPTION));
        Path tmpDir = null;

        try {

            setProxyIfRequired();

            // check credentials if useCache option allows us to download artifacts
            if (useCache != ALWAYS) {
                if (!ARUUtil.checkCredentials(userId, password)) {
                    return new CommandResponse(-1, "User credentials do not match");
                }
            }

            // Step 3: create a tmp directory for user. TODO: make it unique per user
            tmpDir = Files.createTempDirectory(null);
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            System.out.println("tmpDir = " + tmpDirPath);
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();

            List<String> cmdBuilder = getInitialBuildCmd();

            List<String> requiredInstallers = gatherRequiredInstallerKeys();
            for (String eachKey : requiredInstallers) {
                FileResolver fileResolver = new InstallerFile(eachKey, (useCache != ALWAYS), userId, password);
                String targetFilePath = fileResolver.resolve(META_RESOLVER);
                File targetFile = new File(targetFilePath);
                Path targetLink = Files.createLink(Paths.get(tmpDirPath, targetFile.getName()),
                        Paths.get(targetFilePath));
                if (eachKey.contains(InstallerType.JDK.toString())) {
                    cmdBuilder.add("--menu-arg");
                    cmdBuilder.add("JAVA_PKG=" + tmpDir.relativize(targetLink).toString());
                } else if (eachKey.contains(InstallerType.WLS.toString()) ||
                        eachKey.contains(InstallerType.FMW.toString())) {
                    cmdBuilder.add("--menu-arg");
                    cmdBuilder.add("WLS_PKG=" + tmpDir.relativize(targetLink).toString());
                }
            }

            //Copy Dockerfile.create to tmpDir
            Utils.copyResourceAsFile("/Dockerfile.create", tmpDirPath + File.separator + "Dockerfile");
            Utils.copyResourceAsFile("/wls.rsp", tmpDirPath + File.separator + "wls.rsp");
            Utils.copyResourceAsFile("/oraInst.loc", tmpDirPath + File.separator + "oraInst.loc");

            // Step 4: resolve required patches
            List<String> patchKeys = new ArrayList<>();
            if (latestPSU) {
                System.out.println("Getting latest PSU");
                ARUUtil.getAllPSUFor(installerType.toString(), installerVersion, userId, password);

                FileResolver psuResolver = new PatchFile(InstallerType.valueOf(installerType.toString()), installerVersion, (useCache != ALWAYS), userId, password);
                String psuPath = psuResolver.resolve(META_RESOLVER);
                String bugKey = ARUUtil.getLatestPSUFor(installerType.toString(), installerVersion, userId, password);
                System.out.println("LatestPSU for " + installerType + ", bug number: " + bugKey);
                if (bugKey != null) {
                    patchKeys.add(bugKey);
                }
            }

            if (patches != null && !patches.isEmpty()) {
                List<String> bugKeys = ARUUtil.getPatchesFor(installerType.toString(), installerVersion,
                        patches, userId, password);
                patchKeys.addAll(bugKeys);
            }

            for (String patchKey : patchKeys) {
                String patch_path = META_RESOLVER.getValueFromCache(patchKey);
                if (patch_path != null) {
                    File patch_file = new File(patch_path);
                    System.out.println(patch_path + "? exists: " + patch_file.exists()
                            + ", filename: " + patch_file.getName());
                    Files.createLink(Paths.get(toPatchesPath, patch_file.getName()), Paths.get(patch_path));
                } else {
                    logger.severe("Cache does not contain a valid entry for required patch key: " + patchKey);
                }
            }

            if (!patchKeys.isEmpty()) {
                cmdBuilder.add("--menu-arg");
                cmdBuilder.add("PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());
            }

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));

            // Step 6: process builder
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

        //TODO: input validation
//        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
//        Validator validator = validatorFactory.getValidator();
//        Set<ConstraintViolation<User>> userViolations = validator.validate(User.newUser(userId, password));

//        if (userId != null && !userId.isEmpty()) {
//            Pattern emailPattern = Pattern.compile(EMAIL_REGEX);
//            if (emailPattern.matcher(userId).matches()) {
//                USER_SERVICE.addUserSession(new UserSession(User.newUser(userId, password)));
//            } else {
//                throw new IllegalArgumentException(String.format("userId %s is not a valid email format", userId));
//            }
//        }
        Instant endTime = Instant.now();
        return new CommandResponse(0, "menu successful in " + Duration.between(startTime, endTime).getSeconds()  + "s. image tag: " + imageTag);
    }

    private List<String> gatherRequiredInstallerKeys() {
        List<String> retVal = new LinkedList<>();
        retVal.add(String.format("%s_%s", installerType, installerVersion));
        retVal.add(String.format("%s_%s", InstallerType.JDK.toString(), jdkVersion));
        if (DEFAULT_WLS_VERSION.equals(installerVersion) ) {
            retVal.add(OPATCH_1394_KEY);
        }
        return retVal;
    }

//    private List<FileResolver> gatherRequiredInstallers() {
//        List<FileResolver> retVal = new LinkedList<>();
//        retVal.add(new InstallerFile(InstallerType.valueOf(installerType.toString()), installerVersion, (useCache != ALWAYS), userId, password));
//        retVal.add(new InstallerFile(InstallerType.JDK, jdkVersion, (useCache != ALWAYS), userId, password));
//        if (DEFAULT_WLS_VERSION.equals(installerVersion) ) {
//            retVal.add(new InstallerFile(OPATCH_1394_KEY, (useCache != ALWAYS), userId, password));
//        }
//        return retVal;
//    }

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

    private List<String> getInitialBuildCmd() {

        List<String> cmdBuilder = Stream.of("docker", "menu",
                "--squash", "--force-rm", "--no-cache", "--network=host").collect(Collectors.toList());

        cmdBuilder.add("--tag");
        cmdBuilder.add(imageTag);

        if (httpProxyUrl != null && !httpProxyUrl.isEmpty()) {
            cmdBuilder.add("--menu-arg");
            cmdBuilder.add("http_proxy=" + httpProxyUrl);
        } else {
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add("--menu-arg");
                cmdBuilder.add(String.format("http_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }

        if (httpsProxyUrl != null && !httpsProxyUrl.isEmpty()) {
            cmdBuilder.add("--menu-arg");
            cmdBuilder.add("https_proxy=" + httpsProxyUrl);
        } else {
            String proxyHost = System.getProperty("https.proxyHost");
            String proxyPort = System.getProperty("https.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add("--menu-arg");
                cmdBuilder.add(String.format("https_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.set(0, dockerPath.toAbsolutePath().toString());
        }
        return cmdBuilder;
    }

    private void setProxyIfRequired() {
        if (httpProxyUrl != null && !httpProxyUrl.isEmpty()) {
            setSystemProxy(httpProxyUrl, "http");
        }
        if (httpsProxyUrl != null && !httpsProxyUrl.isEmpty()) {
            setSystemProxy(httpsProxyUrl, "https");
        }
    }

    private void setSystemProxy(String proxyUrl, String protocolToSet) {
        try {
            URL url = new URL(proxyUrl);
            String host = url.getHost();
            int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
            String userInfo = url.getUserInfo();
            String protocol = protocolToSet == null ? url.getProtocol() : protocolToSet;

            if (host != null && port != -1) {
                System.setProperty(String.format("%s.proxyHost", protocol), host);
                System.setProperty(String.format("%s.proxyPort", protocol), String.valueOf(port));
                if (userInfo != null) {
                    String[] strings = userInfo.split(":");
                    if (strings.length == 2) {
                        System.setProperty(String.format("%s.proxyUser", protocol), strings[0]);
                        System.setProperty(String.format("%s.proxyPassword", protocol), strings[1]);
                    }
                }
            }
        } catch (MalformedURLException e) {
            String message = String.format(
                    "Exception in setSystemProxy: proxyUrl = %s, protocolToSet = %s, message = %s", proxyUrl,
                    protocolToSet, e.getMessage());
            logger.severe(message);
        }
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
            description = "Tag for the final menu image. Ex: store/oracle/weblogic:12.2.1.3.0"
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
                    "always - use cache always and never download artifacts\n" +
                    "never - never use cache and always download artifacts",
            hidden = true
    )
    private CachePolicy useCache;

    @Option(
            hidden = true,
            names = { "--publish" },
            description = "Publish this docker image"
    )
    private boolean isPublish = false;

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
            description = "file to log output from this tool. This is different from the docker menu log.",
            hidden = true
    )
    private Path toolLog;

    @Option(
            names = { "--dockerLog" },
            description = "file to log output from the docker menu",
            hidden = true
    )
    private Path dockerLog;

    @Option(
            names = {"--docker"},
            description = "path to docker executable. Default: ${DEFAULT-VALUE}",
            defaultValue = "docker"
    )
    private Path dockerPath;

    @Unmatched
    List<String> unmatcheOptions;
    */
}
