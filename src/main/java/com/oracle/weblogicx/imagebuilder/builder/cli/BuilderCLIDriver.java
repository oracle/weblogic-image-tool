package com.oracle.weblogicx.imagebuilder.builder.cli;

import com.oracle.weblogicx.imagebuilder.builder.api.model.*;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.HttpUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.Utils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.impl.service.UserServiceImpl.USER_SERVICE;
import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.OPATCH_1394_URL;

@Command(
        name = "builder",
        mixinStandardHelpOptions = true,
        description = "Build WebLogic docker image",
        version = "1.0",
        sortOptions = false,
        subcommands = { CacheCLI.class },
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class BuilderCLIDriver implements Callable<CommandResponse> {

    @Option(
            names = { "--installerType" },
            description = "Installer type. Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private InstallerType installerType;

    @Option(
            names = { "--installerVersion" },
            description = "Supported values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "12.2.1.3.0",
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
            paramLabel = "<Wait for Prompt>",
            interactive = true,
            required = true,
            description = "Password for support userId"
    )
    private String password;

    @Option(
            names = { "--useCache" },
            paramLabel = "<Cache Policy>",
            defaultValue = "first",
            description = "Whether should use local cache or download artifacts.\n" +
                    "first - try to use cache and download artifacts if required\n" +
                    "always - use cache only and never download artifacts\n" +
                    "never - never use cache and always download artifacts"
    )
    private CachePolicy cachePolicy;

    @Option(
            hidden = true,
            names = { "--publish" },
            description = "Publish this docker image"
    )
    private boolean isPublish = false;

    @Override
    public CommandResponse call() throws Exception {

        Instant startTime = Instant.now();

        System.out.println("hello");
        System.out.println("InstallerType = \"" + installerType + "\"");
        System.out.println("InstallerVersion = \"" + installerVersion + "\"");
        System.out.println("latestPSU = \"" + latestPSU + "\"");
        System.out.println("patches = \"" + patches + "\"");
        System.out.println("fromImage = \"" + fromImage + "\"");
        System.out.println("userId = \"" + userId + "\"");
        System.out.println("password = \"" + password + "\"");
        System.out.println("publish = \"" + isPublish + "\"");

        //TODO: input validation
        User user = User.newUser(userId, password);
        UserSession userSession = USER_SERVICE.getUserSession(user);

        if (userSession == null) {
            return new CommandResponse(-1, "User credentials do not match");
        } else {

            Path tmpDir;

            // Step 3: create a tmp directory for user. TODO: make it unique per user
            tmpDir = Files.createTempDirectory("abc");
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

                List<String> cmdBuilder = new ArrayList<>(Arrays.asList("/usr/local/bin/docker", "build",
                        "--squash", "--force-rm", "--no-cache", "--network=host"));

                String cacheDir = META_RESOLVER.getCacheDir();
                List<String> propKeys = new ArrayList<>(Arrays.asList(wlsKey, jdkKey));

                //Download wls, jdk files if required
                for (String propKey : propKeys) {
                    String propVal = properties.getProperty(propKey);
                    if (propVal == null || propVal.isEmpty()) {
                        return new CommandResponse(-1, String.format("Invalid url %s in builder.properties for key %s",
                                propVal, propKey));
                    }

                    String targetFilePath = cacheDir + File.separator + propVal.substring(propVal.lastIndexOf('/') + 1);
                    File targetFile = new File(targetFilePath);

                    if (!targetFile.exists() || !META_RESOLVER.hasMatchingKeyValue(propKey, targetFilePath)) {
                        System.out.println("1. Downloading from " + propVal + " to " + targetFilePath);
                        HttpUtil.downloadFile(propVal, targetFilePath, userSession);
                        META_RESOLVER.addToCache(propKey, targetFilePath);
                    } else {
                        System.out.println("File exists for key " + propKey + " at location " + targetFilePath);
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
                if ("12.2.1.3.0".equals(installerVersion) ) {
                    if (!opatchFile.exists() || !META_RESOLVER.hasMatchingKeyValue(opatchKey, opatch_1394_path)) {
                        System.out.println("3. Downloading from " + OPATCH_1394_URL + " to " + opatch_1394_path);
                        HttpUtil.downloadFile(OPATCH_1394_URL, opatch_1394_path, userSession);
                        META_RESOLVER.addToCache(opatchKey, opatch_1394_path);
                    } else {
                        System.out.println("File exists for key " + opatchKey + " at location " + opatch_1394_path);
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
                    String bugKey = ARUUtil.getLatestPSUFor(installerType.toString(), installerVersion, userSession);
                    System.out.println("LatestPSU for " + installerType + ", bug number: " + bugKey);
                    patchKeys.add(bugKey);
                }

                if (patches != null && !patches.isEmpty()) {
                    List<String> bugKeys = ARUUtil.getPatchesFor(installerType.toString(), installerVersion, patches,
                        userSession);
                    patchKeys.addAll(bugKeys);
                }

                for (String patchKey : patchKeys) {
                    String patch_path = META_RESOLVER.getValueFromCache(patchKey).get();
                    File patch_file = new File(patch_path);
                    System.out.println(patch_path + "? exists: " + patch_file.exists() + ", filename: " + patch_file.getName());
                    Files.createLink(Paths.get(toPatchesPath, patch_file.getName()), Paths.get(patch_path));
                }

                if (!patchKeys.isEmpty()) {
                    cmdBuilder.add("--build-arg");
                    cmdBuilder.add( "PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());
                }

                System.out.println("PATCHDIR=" + tmpDir.relativize(tmpPatchesDir).toString());

                // Step 5: continue building docker command
                cmdBuilder.add("--tag");
                cmdBuilder.add(imageTag);
                cmdBuilder.add("--build-arg");
                cmdBuilder.add("http_proxy=http://www-proxy-hqdc.us.oracle.com:80");
                cmdBuilder.add("--build-arg");
                cmdBuilder.add("https_proxy=http://www-proxy-hqdc.us.oracle.com:80");

                cmdBuilder.add(tmpDirPath);

                System.out.println("docker cmd = " + String.join(" ", cmdBuilder));

                // Step 6: process builder
                ProcessBuilder processBuilder = new ProcessBuilder(cmdBuilder);
                final Process process = processBuilder.start();

                try(
                BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter logWriter = new BufferedWriter(new FileWriter(new File("/Users/gsuryade/dockerbuild.log")))
                ) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        logWriter.write(line);
                        logWriter.newLine();
                        System.out.println(line);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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
                        .peek(System.out::println)
                        .forEach(File::delete);
            }
        }
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


    static class WLSVersionValues extends ArrayList<String> {
        WLSVersionValues() {
            super(Arrays.asList("12.2.1.3.0", "12.2.1.2.0"));
        }
    }

    static class JDKVersionValues extends ArrayList<String> {
        JDKVersionValues() {
            super(Arrays.asList("7", "8", "9"));
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            CommandLine.usage(new BuilderCLIDriver(), System.out);
        } else {
            CommandResponse response = CommandLine.call(new BuilderCLIDriver(), args);
            if (response != null) {
                System.out.println(String.format("Response code: %d, message: %s", response.getStatus(),
                        response.getMessage()));
            } else {
                System.out.println("response is null");
            }
        }
    }

    /*
    @Override
    public void run() {


        System.out.println("hello");
        System.out.println("InstallerType = \"" + installerType + "\"");
        System.out.println("InstallerVersion = \"" + installerVersion + "\"");
        System.out.println("latestPSU = \"" + latestPSU + "\"");
        System.out.println("patches = \"" + patches + "\"");
        System.out.println("fromImage = \"" + fromImage + "\"");
        System.out.println("userId = \"" + userId + "\"");
        System.out.println("password = \"" + password + "\"");
        System.out.println("publish = \"" + isPublish + "\"");
    }
    */
}
