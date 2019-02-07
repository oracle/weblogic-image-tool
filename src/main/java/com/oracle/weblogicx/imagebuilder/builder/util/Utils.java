/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static boolean doesFileExists(Optional<String> filePath) {
        return filePath.filter(path1 -> new File(path1).exists()).isPresent();
    }

    /**
     * Utility method to copy a resource from the jar to local file system
     * @param resourcePath resource path in the jar
     * @param destPath local file to copy to.
     * @throws IOException in case of error
     */
    public static void copyResourceAsFile(String resourcePath, String destPath) throws IOException {
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
    }

    /**
     * Create a file with the given path.
     * @param filePath the path of the file to create
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

    public static void setProxyIfRequired(String httpProxyUrl, String httpsProxyUrl) throws Exception {
        if (httpProxyUrl != null && !httpProxyUrl.isEmpty()) {
            setSystemProxy(httpProxyUrl, "http");
        }
        if (httpsProxyUrl != null && !httpsProxyUrl.isEmpty()) {
            setSystemProxy(httpsProxyUrl, "https");
        }
    }

    private static void setSystemProxy(String proxyUrl, String protocolToSet) throws Exception {
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
//            String message = String.format(
//                    "Exception in setSystemProxy: proxyUrl = %s, protocolToSet = %s, message = %s", proxyUrl,
//                    protocolToSet, e.getMessage());
            throw e;
        }
    }

    public static void replacePlaceHolders(String destPath, String mainResource, String ...placeHolderResources) throws IOException {
        try (
                PrintWriter printWriter = new PrintWriter(new FileWriter(new File(destPath)))
        ) {
            String mainContent = readResource(mainResource);
            if (mainContent.indexOf("# PLACEHOLDER FOR %%") > 0) {

                Map<String, String> placeHolderMap = new HashMap<>();
                for (String placeHolderResource : placeHolderResources) {
                    placeHolderMap.putAll(readPlaceHolderResource(placeHolderResource));
                }

                for (Map.Entry<String, String> entry : placeHolderMap.entrySet()) {
                    String regex = "# PLACEHOLDER FOR %%" + entry.getKey() + "%% #";
                    mainContent = mainContent.replaceAll(regex, Matcher.quoteReplacement(entry.getValue()));
                }
            }
            printWriter.println(mainContent);
        }
    }

    private static Map<String, String> readPlaceHolderResource(String resourcePath) throws IOException {
        String resourceContent = readResource(resourcePath);
        final String regex = "# START %%(\\S+)%% #(.*)# END %%(\\1)%% #";
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(resourceContent);
        Map<String, String> retMap = new HashMap<>();
        while (matcher.find()) {
            if ( matcher.groupCount() == 3) {
                if (matcher.group(1).equals(matcher.group(3))) {
                    retMap.put(matcher.group(1), matcher.group(2));
                } else {
                    System.out.println("Did not find closing pattern for " + matcher.group(1) + " in resource " +
                            resourcePath);
                }
            } else {
                System.out.println("pattern mismatch in resource " + resourcePath);
            }
        }
        return retMap;
    }

    private static String readResource(String resourcePath) throws IOException {
        try (BufferedReader resourceReader = new BufferedReader(new InputStreamReader(Utils.class.getResourceAsStream(resourcePath)))) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = resourceReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            return stringBuilder.toString();
        }
    }


}
