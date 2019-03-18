/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogicx.imagebuilder.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.weblogicx.imagebuilder.util.Constants.HTTP;
import static com.oracle.weblogicx.imagebuilder.util.Constants.HTTPS;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
     * Utility method to copy a resource from the jar to local file system
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
            Files.setPosixFilePermissions(Paths.get(destPath), PosixFilePermissions.fromString("r-xr-xr-x"));
        }
    }

    /**
     * Create a file with the given path.
     *
     * @param filePath        the path of the file to create
     * @param defaultFileName file name to use in case a directory with the given path exists
     * @return file path or null in case of error
     */
    public static Path createFile(Path filePath, String defaultFileName) {
        Path logFilePath = filePath;
        if (logFilePath != null) {
            try {
                if (!Files.exists(logFilePath)) {
                    Files.createDirectories(logFilePath.getParent());
                    Files.createFile(logFilePath);
                } else {
                    if (Files.isDirectory(logFilePath)) {
                        if (defaultFileName == null || defaultFileName.isEmpty()) {
                            defaultFileName = "log.log";
                        }
                        logFilePath = Paths.get(logFilePath.toAbsolutePath().toString(), defaultFileName);
                        Files.createFile(logFilePath);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logFilePath = null;
            }
        }
        return logFilePath;
    }

    /**
     * Set proxy based on the proxy urls
     *
     * @param httpProxyUrl  http proxy url
     * @param httpsProxyUrl https proxy url
     * @param nonProxyHosts string of non proxy hosts
     * @throws Exception
     */
    public static void setProxyIfRequired(String httpProxyUrl, String httpsProxyUrl, String nonProxyHosts) throws IOException {
        if (!isEmptyString(httpProxyUrl)) {
            setSystemProxy(httpProxyUrl, HTTP);
        }
        if (!isEmptyString(httpsProxyUrl)) {
            setSystemProxy(httpsProxyUrl, HTTPS);
        }
        if (!isEmptyString(nonProxyHosts)) {
            System.setProperty("http.nonProxyHosts", nonProxyHosts.replaceAll("[,;]", "|"));
        }
    }

    private static void setSystemProxy(String proxyUrl, String protocolToSet) throws IOException {
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

    public static void replacePlaceHolders(String destPath, String mainResource, List<String> filterPrefixes, String... placeHolderResources) throws IOException {
        try (
                PrintWriter printWriter = new PrintWriter(new FileWriter(new File(destPath)))
        ) {
            String mainContent = readResource(mainResource);
            if (mainContent.indexOf("# PLACEHOLDER FOR %%") > 0) {

                Map<String, String> placeHolderMap = new HashMap<>();
                for (String placeHolderResource : placeHolderResources) {
                    placeHolderMap.putAll(readPlaceHolderResource(placeHolderResource, filterPrefixes));
                }

                for (Map.Entry<String, String> entry : placeHolderMap.entrySet()) {
                    String regex = MessageFormat.format("# PLACEHOLDER FOR %%{0}%% #", entry.getKey());
                    mainContent = mainContent.replaceAll(regex, Matcher.quoteReplacement(entry.getValue()));
                }
            }
            printWriter.println(mainContent);
        }
    }

    private static Map<String, String> readPlaceHolderResource(String resourcePath, List<String> desiredPrefixes) throws IOException {
        String resourceContent = readResource(resourcePath);
        final String regex = "# START %%(\\S+)%%([_A-Z]*?) #(.*)# END %%(\\1)%%\\2 #";
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(resourceContent);
        Map<String, String> retMap = new HashMap<>();
        while (matcher.find()) {
            if (matcher.groupCount() == 4) {
                String startTag = matcher.group(1);
                String extraIdentifier = matcher.group(2);
                String content = matcher.group(3);
                String endTag = matcher.group(4);
                if (startTag.equals(endTag) && desiredPrefixes.stream().anyMatch(
                        x -> startTag.startsWith(x) || extraIdentifier.equalsIgnoreCase(x))) {
                    retMap.put(startTag, content);
                }
            } else {
                logger.fine("pattern mismatch in resource " + resourcePath);
            }
        }
        return retMap;
    }

    /**
     * Reads a given resource and returns it's content as string
     *
     * @param resourcePath resource to read
     * @return content of the resource
     * @throws IOException
     */
    private static String readResource(String resourcePath) throws IOException {
        try (BufferedReader resourceReader = new BufferedReader(new InputStreamReader(
                Utils.class.getResourceAsStream(resourcePath)))) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = resourceReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Executes the given docker command and writes the process stdout to log
     *
     * @param isCLIMode whether the tool is being run in CLI mode
     * @param cmdBuilder command to execute
     * @param dockerLog  log file to write to
     * @throws IOException
     * @throws InterruptedException
     */
    public static void runDockerCommand(boolean isCLIMode, List<String> cmdBuilder, Path dockerLog)
            throws IOException, InterruptedException {
        // process builder
        ProcessBuilder processBuilder = new ProcessBuilder(cmdBuilder);
        Path dockerLogPath = createFile(dockerLog, "dockerbuild.log");
        List<OutputStream> outputStreams = new ArrayList<>();

        if (isCLIMode) {
            outputStreams.add(System.out);
        }

        if (dockerLogPath != null) {
            logger.info("dockerLog: " + dockerLog);
            outputStreams.add(new FileOutputStream(dockerLogPath.toFile()));
        }

        final Process process = processBuilder.start();
        writeFromInputToOutputStreams(process.getInputStream(), outputStreams.toArray(new OutputStream[0]));
        if (process.waitFor() != 0) {
            processError(process);
        }
    }

    /**
     * Executes the given docker command and returns the stdout of the process as properties
     *
     * @param cmdBuilder command to execute
     * @return properties built from the stdout of the docker command
     * @throws IOException
     * @throws InterruptedException
     */
    public static Properties runDockerCommand(List<String> cmdBuilder) throws IOException, InterruptedException {
        // process builder
        ProcessBuilder processBuilder = new ProcessBuilder(cmdBuilder);
        final Process process = processBuilder.start();
        Properties properties = new Properties();
        try (
                BufferedReader processReader = new BufferedReader(new InputStreamReader(
                        process.getInputStream()))
        ) {
            properties.load(processReader);
        }

        if (process.waitFor() != 0) {
            processError(process);
        }
        return properties;
    }

    /**
     * Throws an Exception if the given process failed with error
     *
     * @param process process
     * @throws IOException
     */
    private static void processError(Process process) throws IOException {
        try (BufferedReader stderr =
                     new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = stderr.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            throw new IOException(
                    "docker command failed with error: " + stringBuilder.toString());
        }
    }

    private static void writeFromInputToOutputStreams(InputStream inputStream, OutputStream... outputStreams) {
        Thread readerThread = new Thread(() -> {
            try(
                    BufferedReader processReader = new BufferedReader(new InputStreamReader(inputStream));
                    CloseableList<PrintWriter> printWriters = createPrintWriters(outputStreams)
            ) {
                if (!printWriters.isEmpty()) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        String finalLine = line;
                        printWriters.forEach(x -> x.println(finalLine));
                    }
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private static CloseableList<PrintWriter> createPrintWriters(OutputStream... outputStreams) {
        CloseableList<PrintWriter> retVal = new CloseableList<>();
        if (outputStreams != null) {
            for (OutputStream outputStream : outputStreams) {
                retVal.add(new PrintWriter(new OutputStreamWriter(outputStream), true));
            }
        }
        return retVal;
    }

    /**
     * Deletes files from given dir and its subdirectories
     *
     * @param tmpDir dir
     * @throws IOException in case of error
     */
    public static void deleteFilesRecursively(Path tmpDir) throws IOException {
        if (tmpDir != null) {
            Files.walk(tmpDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    //.peek(System.out::println)
                    .forEach(File::delete);

            if (Files.exists(tmpDir)) {
                logger.warning("Directory not cleaned up, please remove it manually " + tmpDir.toString());
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
     * and less than zero if thisVersion is older.
     */
    public static int compareVersions(String thisVersion, String otherVersion) {
        int result = 0;

        if (isEmptyString(thisVersion) || isEmptyString(otherVersion)) {
            IllegalArgumentException iae = new IllegalArgumentException("cannot compare null strings");
            throw iae;
        }

        String[] tmp = thisVersion.split("-");
        String strippedThisVersion = tmp[0];
        String[] thisVersionElements = strippedThisVersion.split("\\.");

        tmp = otherVersion.split("-");
        String strippedOtherVersion = tmp[0];
        String[] otherVersionElements = strippedOtherVersion.split("\\.");

        int fieldsToCompare;
        if (thisVersionElements.length <= otherVersionElements.length) {
            fieldsToCompare = thisVersionElements.length;
        } else {
            fieldsToCompare = otherVersionElements.length;
        }

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

    public static String getPackageMgrStr(String osID) {
        String retVal = "_YUM";
        if (osID != null) {
            osID = osID.replaceAll("[\"]", "");
            switch (osID) {
                case "centos":
                case "ol":
                case "rhel":
                    retVal = "_YUM";
                    break;
                case "ubuntu":
                case "debian":
                    retVal = "_APT";
                    break;
                case "opensuse":
                    retVal = "_SUSE";
                    break;
            }
        }
        return retVal;
    }

    /**
     * Constructs a docker command to run a script in the container with a volume mount
     *
     * @param hostDirToMount host dir
     * @param dockerImage    docker image tag
     * @param scriptToRun    script to execute on the container
     * @param args           args to the script
     * @return command
     */
    public static List<String> getDockerRunCmd(Path hostDirToMount, String dockerImage, String scriptToRun,
                                               String... args) {
        //docker run -v /user.dir/tmpdir:/tmp wls122130:min sh -c /tmp/test-env.sh
        final List<String> retVal = Stream.of(
                "docker", "run", "--volume=" + hostDirToMount.toAbsolutePath().toString() + ":/tmp_scripts",
                dockerImage, "/tmp_scripts/" + scriptToRun).collect(Collectors.toList());
        if (args != null && args.length > 0) {
            retVal.addAll(Arrays.asList(args));
        }
        return retVal;
    }

    /**
     * Detect proxy settings if not provided by the user
     *
     * @param proxyUrl url set by the user
     * @param protocol http, https or none
     * @return proxy url for given protocol
     */
    public static String findProxyUrl(String proxyUrl, String protocol) {
        String retVal = proxyUrl;
        if (isEmptyString(retVal) && !isEmptyString(protocol)) {
            switch (protocol.toLowerCase()) {
                case HTTP:
                case HTTPS:
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
     * Utility method to parse password out of three inputs
     * @param passwordStr password in plain string form
     * @param passwordFile file containing just the password
     * @param passwordEnv name of environment variable containing the password
     * @return parsed value
     * @throws IOException in case of error
     */
    public static String getPasswordFromInputs(String passwordStr, Path passwordFile, String passwordEnv) throws IOException {
        if (!isEmptyString(passwordStr)) {
            return passwordStr;
        } else if (passwordFile != null && Files.isRegularFile(passwordFile) && Files.size(passwordFile) > 0) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(passwordFile.toFile()))) {
                return bufferedReader.readLine();
            }
        } else if (!isEmptyString(passwordEnv) && !isEmptyString(System.getenv(passwordEnv))) {
            return System.getenv(passwordEnv);
        }
        throw new IOException("Failed to determine password. use one of the options to input password");
    }
}
