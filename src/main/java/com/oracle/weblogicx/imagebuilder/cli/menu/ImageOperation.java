package com.oracle.weblogicx.imagebuilder.cli.menu;

import com.oracle.weblogicx.imagebuilder.api.FileResolver;
import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.impl.InstallerFile;
import com.oracle.weblogicx.imagebuilder.impl.PatchFile;
import com.oracle.weblogicx.imagebuilder.impl.meta.CacheStoreFactory;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.util.Constants.BUILD_ARG;
import static com.oracle.weblogicx.imagebuilder.util.Constants.HTTP;
import static com.oracle.weblogicx.imagebuilder.util.Constants.HTTPS;

public abstract class ImageOperation implements Callable<CommandResponse> {

    private Logger logger = Logger.getLogger("com.oracle.weblogix.imagebuilder.builder");
    final List<String> filterStartTags = new ArrayList<>();
    String nonProxyHosts = null;

    /**
     * Builds a list of build args to pass on to docker with the required patches.
     * Also, creates links to patches directory under build context instead of copying over.
     * @param tmpDir build context dir
     * @param tmpPatchesDir patches dir under build context
     * @return list of strings
     * @throws Exception in case of error
     */
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        List<String> retVal = new LinkedList<>();
        List<String> patchLocations = new LinkedList<>();
        String toPatchesPath = tmpPatchesDir.toAbsolutePath().toString();

        if (latestPSU) {
            System.out.println("Getting latest PSU");
            FileResolver psuResolver = new PatchFile(installerType.toString(), installerVersion, null, userId, password);
            patchLocations.add(psuResolver.resolve(cacheStore));
        }
        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                patchLocations.add(new PatchFile(installerType.toString(), installerVersion, patchId, userId, password)
                        .resolve(cacheStore));
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
            filterStartTags.add("PATCH_");
        }
        return retVal;
    }

//    /**
//     * Builds a list of {@link InstallerFile} objects based on user input which are processed
//     * to download the required install artifacts
//     * @return list of InstallerFile
//     * @throws Exception in case of error
//     */
//     protected abstract List<InstallerFile> gatherRequiredInstallers() throws Exception;

    /**
     * Builds the options for docker build command
     * @return list of options
     */
    List<String> getInitialBuildCmd() {

        List<String> cmdBuilder = Stream.of("docker", "build",
                "--squash", "--force-rm", "--rm=true", "--no-cache", "--network=host").collect(Collectors.toList());

        cmdBuilder.add("--tag");
        cmdBuilder.add(imageTag);

        if (!Utils.isEmptyString(httpProxyUrl)) {
            cmdBuilder.add(BUILD_ARG);
            cmdBuilder.add("http_proxy=" + httpProxyUrl);
        }
        /*
        else {
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPort = System.getProperty("http.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add(String.format("http_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }*/

        if (!Utils.isEmptyString(httpsProxyUrl)) {
            cmdBuilder.add(BUILD_ARG);
            cmdBuilder.add("https_proxy=" + httpsProxyUrl);
        }
        /*
        else {
            String proxyHost = System.getProperty("https.proxyHost");
            String proxyPort = System.getProperty("https.proxyPort", "80");
            if (proxyHost != null) {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add(String.format("https_proxy=http://%s:%s", proxyHost, proxyPort));
            }
        }*/

        if (!Utils.isEmptyString(nonProxyHosts)) {
            cmdBuilder.add(BUILD_ARG);
            cmdBuilder.add("no_proxy=" + nonProxyHosts);
        }

        if (dockerPath != null && Files.isExecutable(dockerPath)) {
            cmdBuilder.set(0, dockerPath.toAbsolutePath().toString());
        }
        return cmdBuilder;
    }

    void handleProxyUrls() throws IOException {
        httpProxyUrl = Utils.findProxyUrl(httpProxyUrl, HTTP);
        httpsProxyUrl = Utils.findProxyUrl(httpsProxyUrl, HTTPS);
        nonProxyHosts = Utils.findProxyUrl(nonProxyHosts, "none");
        Utils.setProxyIfRequired(httpProxyUrl, httpsProxyUrl, nonProxyHosts);
    }

    void addOPatch1394ToImage(Path tmpDir) throws Exception {
        String filePath = (new PatchFile("opatch", "13.9.4.0.0", "28186730", userId, password)
                .resolve(cacheStore));
        Files.createLink(Paths.get(tmpDir.toAbsolutePath().toString(), new File(filePath).getName()),
                Paths.get(filePath));
        filterStartTags.add("OPATCH_1394");
    }

    /**
     * Enable logging when using cli mode and required log file path is supplied
     * @param isCLIMode whether tool is run in cli mode
     * @return log file handler or null
     */
    FileHandler setupLogger(boolean isCLIMode) {
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

    WLSInstallerType installerType = WLSInstallerType.WLS;

    String installerVersion = Constants.DEFAULT_WLS_VERSION;

    @Option(
            names = { "--latestPSU" },
            description = "Whether to apply patches from latest PSU."
    )
    boolean latestPSU = false;

    @Option(
            names = { "--patches" },
            paramLabel = "patchId",
            split = ",",
            description = "Comma separated patch Ids. Ex: p12345678,p87654321"
    )
    List<String> patches;

    @Option(
            names = { "--tag" },
            paramLabel = "TAG",
            required = true,
            description = "Tag for the final build image. Ex: store/oracle/weblogic:12.2.1.3.0"
    )
    String imageTag;

    @Option(
            names = { "--user" },
            paramLabel = "<support email>",
            description = "Oracle Support email id",
            required = true
    )
    String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<password for support user id>",
            description = "Password for support userId",
            required = true
    )
    String password;

    @Option(
            names = { "--httpProxyUrl" },
            description = "proxy for http protocol. Ex: http://myproxy:80 or http://user:passwd@myproxy:8080"
    )
    String httpProxyUrl;

    @Option(
            names = { "--httpsProxyUrl" },
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
            names = { "--dockerLog" },
            description = "file to log output from the docker build",
            hidden = true
    )
    Path dockerLog;

    @Option(
            names = { "--toolLog" },
            description = "file to log output from this tool. This is different from the docker build log.",
            hidden = true
    )
    private Path toolLog;

//    @Option(
//            names = {"--cacheStoreType"},
//            description = "Whether to use file backed cache store or preferences backed cache store. Ex: file or pref",
//            hidden = true,
//            defaultValue = "file"
//    )
    protected CacheStore cacheStore = new CacheStoreFactory().get();

    @Unmatched
    List<String> unmatchedOptions;
}
