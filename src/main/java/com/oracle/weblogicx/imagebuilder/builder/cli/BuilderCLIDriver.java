/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.cli;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CachePolicy;
import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.api.model.InstallerType;
import com.oracle.weblogicx.imagebuilder.builder.api.model.JDKVersionValues;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSVersionValues;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.HttpUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.builder.api.model.CachePolicy.always;
import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_WLS_VERSION;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.OPATCH_1394_KEY;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.OPATCH_1394_URL;

@Command(
        name = "build",
        mixinStandardHelpOptions = true,
        description = "Build WebLogic docker image",
        version = "1.0",
        sortOptions = false,
        //subcommands = { CacheCLI.class },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class BuilderCLIDriver implements Callable<CommandResponse> {

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

        FileHandler fileHandler = setupLogger(cliMode);
        setProxyIfRequired();

        // check credentials if useCache option allows us to download artifacts
        if (useCache != always) {
            if (!ARUUtil.checkCredentials(userId, password)) {
                return new CommandResponse(-1, "User credentials do not match");
            }
        }

        List<String> requiredKeys = gatherRequiredBuildProps();

        // Step 3: create a tmp directory for user. TODO: make it unique per user
        Path tmpDir = Files.createTempDirectory(null);
        String tmpDirPath = tmpDir.toAbsolutePath().toString();
        System.out.println("tmpDir = " + tmpDirPath);
        Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
        String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();

        // Step 1: read builder.properties
        try(InputStream inputStream = BuilderCLIDriver.class.getResourceAsStream("/builder.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);

            String wlsKey = String.format("%s_%s_url", installerType, installerVersion);
            String jdkKey = String.format("%s_%s_url", "jdk", "8");

            List<String> cmdBuilder = getInitialBuildCmd();

            String cacheDir = META_RESOLVER.getCacheDir();
            List<String> propKeys = new ArrayList<>(Arrays.asList(wlsKey, jdkKey));

            //Download WLS, jdk files if required
            for (String propKey : propKeys) {
                String propVal = properties.getProperty(propKey);
                if (propVal == null || propVal.isEmpty()) {
                    return new CommandResponse(-1, String.format("Invalid url %s in builder.properties for key %s",
                            propVal, propKey));
                }

                String targetFilePath = cacheDir + File.separator + propVal.substring(propVal.lastIndexOf('/') + 1);
                File targetFile = new File(targetFilePath);

                if (!targetFile.exists() || !META_RESOLVER.hasMatchingKeyValue(propKey, targetFilePath)) {
                    if (useCache != always) {
                        System.out.println("1. Downloading from " + propVal + " to " + targetFilePath);
                        logger.info("1. Downloading from " + propVal + " to " + targetFilePath);
                        HttpUtil.downloadFile(propVal, targetFilePath, userId, password);
                        META_RESOLVER.addToCache(propKey, targetFilePath);
                    } else {
                        return new CommandResponse(-1, String.format(
                                "--useCache set to %s. Need to download file from %s", useCache, propVal));
                    }
                }

                Path targetLink = Files.createLink(Paths.get(tmpDirPath, targetFile.getName()),
                        Paths.get(targetFilePath));
                cmdBuilder.add("--build-arg");
                cmdBuilder.add((propKey.contains("jdk_")? "JAVA_PKG=" : "WLS_PKG=") +
                        tmpDir.relativize(targetLink).toString());
            }

            //OPatch patch 13.9.4
            final String opatchKey = "opatch_1394";
            String opatch_1394_path = cacheDir + File.separator + "p28186730_139400_Generic.zip";
            File opatchFile = new File(opatch_1394_path);
            if (DEFAULT_WLS_VERSION.equals(installerVersion) ) {
                if (!opatchFile.exists() || !META_RESOLVER.hasMatchingKeyValue(opatchKey, opatch_1394_path)) {
                    if (useCache != always) {
                        System.out.println("3. Downloading from " + OPATCH_1394_URL + " to " + opatch_1394_path);
                        logger.info("3. Downloading from " + OPATCH_1394_URL + " to " + opatch_1394_path);
                        HttpUtil.downloadFile(OPATCH_1394_URL, opatch_1394_path, userId, password);
                        META_RESOLVER.addToCache(opatchKey, opatch_1394_path);
                    } else {
                        return new CommandResponse(-1, String.format(
                                "--useCache set to %s. Need to download file from %s", useCache, OPATCH_1394_URL));
                    }
                }
                Files.createLink(Paths.get(tmpDirPath, opatchFile.getName()), Paths.get(opatch_1394_path));
            }

            //Copy Dockerfile to tmpDir
            Utils.copyResourceAsFile("/Dockerfile", tmpDirPath + File.separator + "Dockerfile");
            Utils.copyResourceAsFile("/wls.rsp", tmpDirPath + File.separator + "wls.rsp");
            Utils.copyResourceAsFile("/oraInst.loc", tmpDirPath + File.separator + "oraInst.loc");

            // Step 4: resolve required patches
            List<String> patchKeys = new ArrayList<>();
            if (latestPSU) {
                System.out.println("Getting latest PSU");
                String bugKey = ARUUtil.getLatestPSUFor(installerType.toString(), installerVersion, userId, password);
                System.out.println("LatestPSU for " + installerType + ", bug number: " + bugKey);
                patchKeys.add(bugKey);
            }

            if (patches != null && !patches.isEmpty()) {
                List<String> bugKeys = ARUUtil.getPatchesFor(installerType.toString(), installerVersion,
                        patches, userId, password);
                patchKeys.addAll(bugKeys);
            }

            for (String patchKey : patchKeys) {
                String patch_path = META_RESOLVER.getValueFromCache(patchKey);
                File patch_file = new File(patch_path);
                System.out.println(patch_path + "? exists: " + patch_file.exists() + ", filename: " + patch_file.getName());
                Files.createLink(Paths.get(toPatchesPath, patch_file.getName()), Paths.get(patch_path));
            }

            if (!patchKeys.isEmpty()) {
                cmdBuilder.add("--build-arg");
                cmdBuilder.add( "PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());
            }

            System.out.println("PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());
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
            ex.printStackTrace();
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            Files.walk(tmpDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    //.peek(System.out::println)
                    .forEach(File::delete);
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
        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds()  + "s. image tag: " + imageTag);
    }

    private List<String> gatherRequiredBuildProps() {
        List<String> retVal = new LinkedList<>();
        retVal.add(String.format("%s_%s", installerType, installerVersion));
        retVal.add(String.format("%s_%s", InstallerType.JDK.toString(), jdkVersion));
        if (DEFAULT_WLS_VERSION.equals(installerVersion) ) {
            retVal.add(OPATCH_1394_KEY);
        }
        return retVal;
    }

/*    private Properties readBuildProperties() throws IOException {
        Properties properties = new Properties();
        try(InputStream inputStream = BuilderCLIDriver.class.getResourceAsStream("/builder.properties")) {
            properties.load(inputStream);
        }
        return properties;
    }*/

/*    private Map<String, String> getRequiredKeyValues(Set<String> keys, Properties source) throws Exception {
        Map<String, String> retMap = new HashMap<>();
        Set<String> missingKeys = keys.stream().filter(x -> source.getProperty(x) == null).collect(Collectors.toSet());

        if (missingKeys.isEmpty()) {
            for ( String key : keys ) {
                String value = source.getProperty(key);
            }
            retMap = keys.stream().collect(Collectors.toMap(x, source.getProperty(x)));
        } else {
            throw new Exception("Missing values for keys: " + missingKeys);
        }
    }*/

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
            //supress exception
            fileHandler = null;
        }
        return fileHandler;
    }

    private List<String> getInitialBuildCmd() {
        List<String> cmdBuilder = Stream.of("docker", "build",
                "--squash", "--force-rm", "--no-cache", "--network=host").collect(Collectors.toList());

        cmdBuilder.add("--tag");
        cmdBuilder.add(imageTag);

        if (httpProxyUrl != null && !httpProxyUrl.isEmpty()) {
            cmdBuilder.add("--build-arg");
            cmdBuilder.add("http_proxy=" + httpProxyUrl);
        } else {
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add("--build-arg");
                cmdBuilder.add(String.format("http_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }

        if (httpsProxyUrl != null && !httpsProxyUrl.isEmpty()) {
            cmdBuilder.add("--build-arg");
            cmdBuilder.add("https_proxy=" + httpsProxyUrl);
        } else {
            String proxyHost = System.getProperty("https.proxyHost");
            String proxyPort = System.getProperty("https.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add("--build-arg");
                cmdBuilder.add(String.format("https_proxy=http://%s:%s", proxyHost, proxyPort));
            }
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
            description = "Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "8",
            completionCandidates = JDKVersionValues.class
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
            description = "Your WebLogic docker image to use as base image.",
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
            required = true,
            description = "Your Oracle Support email id"
    )
    private String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<password associated with support user id>",
            required = true,
            description = "Password for support userId"
    )
    private String password;

    @Option(
            names = { "--useCache" },
            paramLabel = "<Cache Policy>",
            defaultValue = "first",
            description = "Whether to use local cache or download artifacts.\n" +
                    "first - try to use cache and download artifacts if required\n" +
                    "always - use cache always and never download artifacts\n" +
                    "never - never use cache and always download artifacts"
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
            description = "file to log output from this tool. This is different from the docker build log."
    )
    private Path toolLog;

    @Option(
            names = { "--dockerLog" },
            description = "file to log output from the docker build"
    )
    private Path dockerLog;

    @Option(
            names = { "--cli" },
            description = "CLI Mode",
            hidden = true
    )
    private boolean cliMode;

//    static class WLSVersionValues extends ArrayList<String> {
//        WLSVersionValues() {
//            super(Arrays.asList(DEFAULT_WLS_VERSION, "12.2.1.2.0"));
//        }
//    }

//    static class JDKVersionValues extends ArrayList<String> {
//        JDKVersionValues() {
//            super(Arrays.asList("7", "8"));
//        }
//    }

    public static void main(String[] args) {
        if (args.length == 0) {
            CommandLine.usage(new BuilderCLIDriver(), System.out);
        } else {
            List<String> argsList = Stream.of(args).collect(Collectors.toList());
            argsList.add("--cli");
//            CommandLine commandLine = new CommandLine(new BuilderCLIDriver())
//                    .setCaseInsensitiveEnumValuesAllowed(true)
//                    .setUsageHelpWidth(120);
//
//            List<Object> results = commandLine.parseWithHandlers(
//                    new CommandLine.RunLast().useOut(System.out).useAnsi(CommandLine.Help.Ansi.AUTO),
//                    new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(System.err).useAnsi(CommandLine.Help.Ansi.AUTO),
//                    argsList.toArray(new String[0]));
//
//            CommandResponse response = (results == null || results.isEmpty())? null : (CommandResponse) results.get(0);

            CommandResponse response = WLSCommandLine.call(new BuilderCLIDriver(), argsList.toArray(new String[0]));
            if (response != null) {
                System.out.println(String.format("Response code: %d, message: %s", response.getStatus(),
                        response.getMessage()));
            } else {
                System.out.println("response is null");
            }
        }
    }

}
