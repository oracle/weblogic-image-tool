// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

public class Utils {

    private static final LoggingFacade logger = LoggingFactory.getLogger(Utils.class);

    @NonNls
    private static ResourceBundle bundle = ResourceBundle.getBundle("ImageTool");

    private Utils() {
        // hide constructor, usage of this class is only static utilities
    }

    /**
     * Utility method to copy a resource from the jar to local file system.
     *
     * @param resourcePath resource path in the jar
     * @param destPath     local file to copy to.
     * @param markExec     sets the executable flag if true
     * @throws IOException in case of error
     */
    public static void copyResourceAsFile(String resourcePath, String destPath, boolean markExec) throws IOException {
        Objects.requireNonNull(resourcePath);
        Objects.requireNonNull(destPath);
        if (Files.isDirectory(Paths.get(destPath))) {
            if (resourcePath.contains("/")) {
                destPath = destPath + File.separator + resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
            } else {
                destPath = destPath + File.separator + resourcePath;
            }
        }
        Files.copy(Utils.class.getResourceAsStream(resourcePath), Paths.get(destPath),
            StandardCopyOption.REPLACE_EXISTING);
        if (markExec) {
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                Files.setPosixFilePermissions(Paths.get(destPath), PosixFilePermissions.fromString("r-xr-xr-x"));
            }
        }
    }

    /**
     * Utility method to copy a local file to another local file system location.
     *
     * @param sourcePath   resource path in the local directory
     * @param destPath     local file to copy to.
     * @param markExec     sets the executable flag if true
     * @throws IOException in case of error
     */
    public static void copyLocalFile(Path sourcePath, Path destPath, boolean markExec) throws IOException {
        Objects.requireNonNull(sourcePath);
        Objects.requireNonNull(destPath);
        logger.fine("copyLocalFile: copying file {0}->{1}", sourcePath, destPath);
        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        if (markExec) {
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                Files.setPosixFilePermissions(destPath, PosixFilePermissions.fromString("r-xr-xr-x"));
            }
        }
    }

    /**
     * Utility method to copy a local directory to another local file system location.
     *
     * @param sourcePath   path to the local directory
     * @param destPath     local directory to copy to.
     * @param markExec     sets the executable flag if true
     * @throws IOException in case of error
     */
    public static void copyLocalDirectory(Path sourcePath, Path destPath, boolean markExec) throws IOException {
        Objects.requireNonNull(sourcePath);
        Objects.requireNonNull(destPath);
        if (!Files.isDirectory(sourcePath)) {
            throw new IllegalArgumentException(getMessage("IMG-0007", sourcePath.toString()));
        }

        logger.fine("copyLocalDirectory: copying folder {0}->{1}", sourcePath, destPath);

        // retain folder structure of source in destination folder
        Files.createDirectory(destPath);

        // get children of source directory and copy them to destination directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    copyLocalDirectory(child, destPath.resolve(child.getFileName()), markExec);
                } else if (Files.isRegularFile(child)) {
                    copyLocalFile(child, destPath.resolve(child.getFileName()), markExec);
                } else {
                    logger.info("IMG-0035", sourcePath.toString());
                }
            }
        }

    }

    /**
     * Set proxy based on the proxy urls.
     *
     * @param httpProxyUrl  http proxy url
     * @param httpsProxyUrl https proxy url
     * @param nonProxyHosts string of non proxy hosts
     * @throws IOException when HTTP/HTTPS call has a low level error.
     */
    public static void setProxyIfRequired(String httpProxyUrl, String httpsProxyUrl, String nonProxyHosts)
        throws IOException {
        if (!isEmptyString(httpProxyUrl)) {
            setSystemProxy(httpProxyUrl, Constants.HTTP);
        }
        if (!isEmptyString(httpsProxyUrl)) {
            setSystemProxy(httpsProxyUrl, Constants.HTTPS);
        }
        if (!isEmptyString(nonProxyHosts)) {
            System.setProperty("http.nonProxyHosts", nonProxyHosts.replaceAll("[,;]", "|"));
        }
    }

    private static void setSystemProxy(String proxyUrl, String protocolToSet) throws IOException {

        // If no scheme is set then default to http://
        if (!proxyUrl.toLowerCase().startsWith(Constants.HTTP + "://")
            && !proxyUrl.toLowerCase().startsWith(Constants.HTTPS + "://")) {
            proxyUrl =  Constants.HTTP + "://" + proxyUrl;
        }
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
    }

    /**
     * Create the Dockerfile to be used by the docker build command for this run.
     *
     * @param destPath the file folder that the Dockerfile should be written to.
     * @param template the Dockerfile template that should be used to create the Dockerfile.
     * @param options  the options to be applied to the Dockerfile template.
     * @param dryRun   when true, will return a String version of the Dockerfile.
     * @return null if dryRun is false, and a String version of the Dockerfile if dryRun is true.
     * @throws IOException if an error occurs in the low level Java file operations.
     */
    public static String writeDockerfile(String destPath, String template, DockerfileOptions options, boolean dryRun)
        throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory("docker-files");
        Mustache mustache = mf.compile(template);
        try (FileWriter fw = new FileWriter(destPath)) {
            mustache.execute(fw, options).flush();
        }

        if (dryRun) {
            return mustache.execute(new StringWriter(), options).toString();
        } else {
            return null;
        }
    }

    /**
     * Resolve the parameters in the list of Mustache templates with values passed in command line
     * arguments or other values described by the image tool.
     * @param paths list of file paths that are mustache templates
     * @param options list of option files to resolve the mustache parameters
     * @throws IOException if a file in the fileNames is invalid
     */
    public static void writeResolvedFiles(List<Path> paths, List<Object> options)
        throws IOException {
        if (paths != null) {
            for (Path path : paths) {
                logger.fine("writeResolvedFiles: resolve parameters in file {0}", path);
                File directory = path.toFile().getParentFile();
                if (directory == null
                    || !(Files.exists(path) && Files.isReadable(path) && Files.isWritable(path))) {
                    throw new IllegalArgumentException(getMessage("IMG-0073", path));
                }

                MustacheFactory mf = new DefaultMustacheFactory(directory);
                Mustache mustache;
                try (FileReader fr = new FileReader(path.toFile())) {
                    mustache = mf.compile(fr, path.getFileName().toString());
                }

                try (FileWriter fw = new FileWriter(path.toFile())) {
                    mustache.execute(fw, options).flush();
                }
            }
        }

    }

    /**
     * Executes the given docker command and returns the stdout of the process as properties.
     *
     * @param cmdBuilder command to execute
     * @return properties built from the stdout of the docker command
     * @throws IOException          if an error occurs reading from the process inputstream.
     * @throws InterruptedException when the process wait is interrupted.
     */
    private static Properties runDockerCommand(List<String> cmdBuilder) throws IOException, InterruptedException {
        logger.entering(cmdBuilder);
        // process builder
        ProcessBuilder processBuilder = new ProcessBuilder(cmdBuilder);
        final Process process = processBuilder.start();
        Properties properties = new Properties();
        try (
            BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        ) {
            properties.load(processReader);
        }

        if (process.waitFor() != 0) {
            processError(process);
        }
        logger.exiting(properties);
        return properties;
    }

    /**
     * Throws an Exception if the given process failed with error.
     *
     * @param process the Docker process
     * @throws IOException if an error occurs while reading standard error (stderr) from the Docker build.
     */
    static void processError(Process process) throws IOException {
        try (
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        ) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = stderr.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            throw new IOException("docker command failed with error: " + stringBuilder.toString());
        }
    }

    /**
     * Deletes files from given dir and its subdirectories.
     *
     * @param pathDir dir
     * @throws IOException in case of error
     */
    public static void deleteFilesRecursively(String pathDir) throws IOException {
        if (pathDir != null) {
            Path tmpDir = Paths.get(pathDir);
            try (Stream<Path> walk = Files.walk(tmpDir)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    //.peek(System.out::println)
                    .forEach(File::delete);
            }

            if (Files.exists(tmpDir)) {
                logger.warning("Unable to cleanup temp directory, it is safe to remove this directory manually "
                    + tmpDir.toString());
            }
        }
    }

    /**
     * Compares two version strings.  Any qualifiers are treated as older than the same version without
     * a qualifier.  If both versions have qualifiers and are otherwise equal, they are compared using
     * String.compareTo() to determine the result.
     *
     * @param thisVersion  - first version
     * @param otherVersion - second version
     * @return returns 0 if the versions are equal, greater than zero if thisVersion is newer,
     *     and less than zero if thisVersion is older.
     */
    public static int compareVersions(String thisVersion, String otherVersion) {
        int result = 0;

        if (isEmptyString(thisVersion) || isEmptyString(otherVersion)) {
            throw new IllegalArgumentException("cannot compare null strings");
        }

        String[] tmp = thisVersion.split("-");
        String strippedThisVersion = tmp[0];
        String[] thisVersionElements = strippedThisVersion.split("\\.");

        tmp = otherVersion.split("-");
        String strippedOtherVersion = tmp[0];
        String[] otherVersionElements = strippedOtherVersion.split("\\.");

        int fieldsToCompare = Math.min(thisVersionElements.length, otherVersionElements.length);

        int idx;
        for (idx = 0; idx < fieldsToCompare; idx++) {
            int thisVersionNumber = Integer.parseInt(thisVersionElements[idx]);
            int otherVersionNumber = Integer.parseInt(otherVersionElements[idx]);

            if (thisVersionNumber > otherVersionNumber) {
                result = 1;
                break;
            } else if (thisVersionNumber < otherVersionNumber) {
                result = -1;
                break;
            }
        }

        // Version fields compared so far are equal so check to see if one version number
        // has more fields than the other.
        //
        if (result == 0 && thisVersionElements.length != otherVersionElements.length) {
            if (thisVersionElements.length > otherVersionElements.length) {
                result = 1;
            } else {
                result = -1;
            }
        }

        // Finally, look to see if one or both versions have a qualifier if they are otherwise the same.
        //
        if (result == 0) {
            int useCase = 0;
            if (thisVersion.indexOf('-') != -1) {
                useCase += 1;
            }
            if (otherVersion.indexOf('-') != -1) {
                useCase += 2;
            }
            switch (useCase) {
                case 0:
                    break;

                case 1:
                    result = -1;
                    break;

                case 2:
                    result = 1;
                    break;

                case 3:
                    String thisQualifier = thisVersion.substring(thisVersion.indexOf('-'));
                    String otherQualifier = otherVersion.substring(otherVersion.indexOf('-'));
                    result = thisQualifier.compareTo(otherQualifier);
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    /**
     * Determines if the string is null or empty.
     *
     * @param s the string to check
     * @return whether or not the string is null or empty
     */
    public static boolean isEmptyString(String s) {
        return (s == null) || s.isEmpty();
    }

    /**
     * Reads the docker image environment variables into Java Properties.
     *
     * @param dockerImage the name of the Docker image to read from
     * @param tmpDir      the directory to use on the container to copy a script
     * @return The key/value pairs representing the ENV of the Docker image
     * @throws IOException          when the Docker command fails
     * @throws InterruptedException when the Docker command is interrupted
     */
    public static Properties getBaseImageProperties(String dockerImage, String tmpDir)
        throws IOException, InterruptedException {

        logger.entering(dockerImage, tmpDir);
        List<String> imageEnvCmd = Utils.getDockerRunCmd(tmpDir, dockerImage, "test-env.sh");
        Properties result = Utils.runDockerCommand(imageEnvCmd);
        logger.exiting(result);
        return result;
    }

    /**
     * Constructs a docker command to run a script in the container with a volume mount.
     *
     * @param hostDirToMount host dir
     * @param dockerImage    docker image tag
     * @param scriptToRun    script to execute on the container
     * @param args           args to the script
     * @return command
     */
    private static List<String> getDockerRunCmd(String hostDirToMount, String dockerImage, String scriptToRun,
                                               String... args) throws IOException {

        // We are removing the volume mount option, -v won't work in remote docker daemon and also
        // problematic if the mounted volume source is on a nfs volume as we have no idea what the docker volume
        // driver is.
        //
        // Now are encoding the test script and decode on the fly and execute it.
        // Warning:  don't pass in a big file

        byte[] fileBytes = Files.readAllBytes(Paths.get(hostDirToMount + File.separator + scriptToRun));
        String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
        String oneCommand = String.format("echo %s | base64 -d | /bin/bash", encodedFile);
        logger.finer("ONE COMMAND [" + oneCommand + "]");
        final List<String> retVal = Stream.of(
            "docker", "run", "--rm",
            dockerImage, "/bin/bash", "-c", oneCommand).collect(Collectors.toList());
        if (args != null && args.length > 0) {
            retVal.addAll(Arrays.asList(args));
        }
        return retVal;
    }

    /**
     * Detect proxy settings if not provided by the user.
     *
     * @param proxyUrl url set by the user
     * @param protocol http, https or none
     * @return proxy url for given protocol
     */
    public static String findProxyUrl(String proxyUrl, String protocol) {
        String retVal = proxyUrl;
        if (isEmptyString(retVal) && !isEmptyString(protocol)) {
            switch (protocol.toLowerCase()) {
                case Constants.HTTP:
                case Constants.HTTPS:
                    retVal = System.getenv(String.format("%s_proxy", protocol));
                    if (isEmptyString(retVal)) {
                        String proxyHost = System.getProperty(String.format("%s.proxyHost", protocol), null);
                        String proxyPort = System.getProperty(String.format("%s.proxyPort", protocol), "80");
                        if (proxyHost != null) {
                            retVal = String.format("%s://%s:%s", protocol, proxyHost, proxyPort);
                        }
                    }
                    break;
                case "none":
                default:
                    retVal = System.getenv("no_proxy");
                    if (isEmptyString(retVal)) {
                        retVal = System.getProperty("http.nonProxyHosts", null);
                        if (!isEmptyString(retVal)) {
                            retVal = retVal.replaceAll("\\|", ",");
                        }
                    }
                    break;
            }
        }
        return retVal;
    }

    /**
     * Utility method to parse password out of three inputs.
     *
     * @param passwordStr  password in plain string form
     * @param passwordFile file containing just the password
     * @param passwordEnv  name of environment variable containing the password
     * @return parsed value
     * @throws IOException in case of error
     */
    public static String getPasswordFromInputs(String passwordStr, Path passwordFile, String passwordEnv)
        throws IOException {
        if (!isEmptyString(passwordStr)) {
            return passwordStr;
        } else if (validFile(passwordFile)) {
            try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(passwordFile.toFile()))
            ) {
                return bufferedReader.readLine();
            }
        } else if (!isEmptyString(passwordEnv) && !isEmptyString(System.getenv(passwordEnv))) {
            return System.getenv(passwordEnv);
        }
        return null;
    }

    /**
     * Get the named property from system environment or Java system property.
     * If the property is defined in the Environment, that value will take precedence over
     * Java properties.
     *
     * @param name the name of the environment variable, or Java property
     * @return the value defined in the env or system property
     */
    public static String getEnvironmentProperty(String name) {
        String result = System.getenv(name);
        if (isEmptyString(result)) {
            result = System.getProperty(name);
        }
        return result;
    }

    /**
     * returns the working dir for docker build.
     *
     * @return working directory
     */
    public static String getBuildWorkingDir() throws IOException {
        String workingDir = getEnvironmentProperty("WLSIMG_BLDDIR");
        if (workingDir == null) {
            workingDir = System.getProperty("user.home");
        }
        Path path = Paths.get(workingDir);

        boolean pathExists = Files.exists(path, LinkOption.NOFOLLOW_LINKS);

        if (!pathExists) {
            throw new IOException("Working Directory does not exists " + workingDir);
        } else {
            if (!Files.isDirectory(path)) {
                throw new IOException("Working Directory specified is not a directory " + workingDir);
            }
            if (!Files.isWritable(path)) {
                throw new IOException("Working Directory specified is not writable " + workingDir);
            }
        }

        return workingDir;
    }

    /**
     * Return the version number inside a opatch file.
     *
     * @param fileName full path to the opatch patch
     * @return version number of the patch
     */

    public static String getOpatchVersionFromZip(String fileName) {

        try (ZipFile zipFile = new ZipFile(fileName)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith("/version.txt")) {
                    InputStream stream = zipFile.getInputStream(entry);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        out.append(line);
                    }
                    return out.toString();
                }
            }
        } catch (IOException ioe) {
            logger.warning("Cannot read opatch file " + fileName);
            logger.finer(ioe.getLocalizedMessage());
        }
        return null;
    }

    /**
     * validatePatchIds validate the format of the patch ids.
     *
     * @param patches list of patch ids
     * @return true if all patch IDs are valid , false otherwise.
     */

    public static boolean validatePatchIds(List<String> patches, boolean rigid) {
        Pattern patchIdPattern;
        if (rigid) {
            patchIdPattern = Pattern.compile(Constants.RIGID_PATCH_ID_REGEX);
        } else {
            patchIdPattern = Pattern.compile(Constants.PATCH_ID_REGEX);
        }
        if (patches != null && !patches.isEmpty()) {
            for (String patchId : patches) {
                logger.finer("pattern matching patchId: {0}", patchId);
                Matcher matcher = patchIdPattern.matcher(patchId);
                if (!matcher.matches()) {
                    String errorFormat;
                    if (rigid) {
                        errorFormat = "12345678_12.2.1.3.0";
                    } else {
                        errorFormat = "12345678[_12.2.1.3.0]";
                    }

                    logger.severe("IMG-0004", patchId, errorFormat);
                    return false;
                }
            }

        }

        return true;
    }

    /**
     * Set the Oracle Home directory based on the first installer response file.
     * If no Oracle Home is provided in the response file, do nothing and accept the default value.
     *
     * @param responseFiles installer response files to parse.
     * @param options           Dockerfile options to use for the build (holds the Oracle Home argument)
     */
    public static void setOracleHome(List<Path> responseFiles, DockerfileOptions options) throws IOException {
        if (responseFiles == null || responseFiles.size() < 1) {
            return;
        }
        Path installerResponseFile = responseFiles.get(0);
        Pattern pattern = Pattern.compile("^\\s*ORACLE_HOME=(.*)?");
        Matcher matcher = pattern.matcher("");
        logger.finer("Reading installer response file: {0}", installerResponseFile.getFileName());

        try (BufferedReader reader = new BufferedReader(new FileReader(installerResponseFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.finest("Read response file line: {0}", line);

                matcher.reset(line);
                if (matcher.find()) {
                    String oracleHome = matcher.group(1);
                    if (oracleHome != null) {
                        options.setOracleHome(oracleHome);
                        logger.info("IMG-0010", oracleHome);
                    }
                    break;
                }
            }
        } catch (FileNotFoundException notFound) {
            logger.severe("Unable to find installer response file: {0}", installerResponseFile);
            throw notFound;
        }
    }


    /**
     * Set the Inventory Location directory based on the inventory pointer  file.
     *
     * @param inventoryLocFile installer response file to parse.
     * @param options           Dockerfile options to use for the build (holds the Oracle Home argument)
     */
    public static void setInventoryLocation(String inventoryLocFile, DockerfileOptions options) throws IOException {
        if (inventoryLocFile == null) {
            return;
        }
        Path inventoryLoc = Paths.get(inventoryLocFile);
        Pattern pattern = Pattern.compile("^\\s*inventory_loc=(.*)?");
        Matcher matcher = pattern.matcher("");
        logger.finer("Reading inventory location file: {0}", inventoryLoc.getFileName());

        try (BufferedReader reader = new BufferedReader(new FileReader(inventoryLoc.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.finer("Read inventory loc file line: {0}", line);

                matcher.reset(line);
                if (matcher.find()) {
                    String invLoc = matcher.group(1);
                    if (invLoc != null) {
                        logger.fine("inventory location file specified new inventory location: {0}", invLoc);
                        options.setOraInvDir(invLoc);
                    }
                    break;
                }
            }
        } catch (FileNotFoundException notFound) {
            logger.severe("IMG-0036", inventoryLoc);
            throw notFound;
        }
    }




    private static boolean validFile(Path file) throws IOException {
        return file != null && Files.isRegularFile(file) && Files.size(file) > 0;
    }

    /**
     * Get a message from the resource bundle and format the message with parameters.
     *
     * @param key    message key into the bundle
     * @param params parameters to be applied to the message
     * @return formatted message string including parameters
     */
    public static String getMessage(@PropertyKey(resourceBundle = "ImageTool") String key, Object... params) {
        String message = key;
        if (bundle != null && bundle.containsKey(key)) {
            message = bundle.getString(key);
        }
        if (params == null || params.length == 0) {
            return message;
        } else {
            return MessageFormat.format(message, params);
        }
    }

    /**
     * Remove the intermediate images created by the multi-stage Docker build.
     * @param buildId the build ID used to identify the images created during this build.
     * @throws IOException if the external Docker command fails.
     * @throws InterruptedException if this program was interrupted waiting on the Docker command.
     */
    public static void removeIntermediateDockerImages(String buildId) throws IOException, InterruptedException {
        logger.entering();
        final List<String> command = Stream.of(
            "docker", "image", "prune", "-f", "--filter", "label=com.oracle.weblogic.imagetool.buildid=" + buildId)
            .collect(Collectors.toList());
        Properties result = runDockerCommand(command);
        logger.fine("Intermediate images removed: {0}", result.get("Total"));
        logger.exiting();
    }

    /**
     * Find PSU version from inventory.
     */
    public static String getPsuVersion(String lsInventory) {
        logger.entering();
        String result = null;
        // search inventory for PSU and extract PSU version, if available
        Pattern patternOne = Pattern.compile(
            "WLS PATCH SET UPDATE (\\d+\\.\\d+\\.\\d+\\.\\d+\\.)\\d+\\(ID:(\\d+)\\.\\d+\\)");
        Pattern patternTwo = Pattern.compile(
            "WLS PATCH SET UPDATE (\\d+\\.\\d+\\.\\d+\\.\\d+\\.[1-9]\\d+)");
        for (String line: lsInventory.split("\\n")) {
            Matcher matchPatternOne = patternOne.matcher(line);
            Matcher matchPatternTwo = patternTwo.matcher(line);
            if (matchPatternOne.find()) {
                result = matchPatternOne.group(1) + matchPatternOne.group(2);
                logger.fine("Found PSU in inventory {0}, in {1}", result, line);
                break;
            } else if (matchPatternTwo.find()) {
                result = matchPatternTwo.group(1);
                logger.fine("Found PSU in inventory {0}, in {1}", result, line);
                break;
            }
        }
        logger.exiting(result);
        return result;
    }

    /**
     * Create a new list from an existing collection and adding additional elements, if desired.
     * @param start a set of elements to start from
     * @param elements zero to many additional elements to add to the new list
     * @param <T> the class of the elements to add
     * @return a new list of element T
     */
    @SafeVarargs
    public static <T> List<T> list(Collection<? extends T> start, T... elements) {
        List<T> result = new ArrayList<>(start);
        Collections.addAll(result, elements);
        return result;
    }
}
