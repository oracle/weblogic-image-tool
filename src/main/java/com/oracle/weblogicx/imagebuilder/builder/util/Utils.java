/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Utils {

    public static boolean doesFileExists(Optional<String> filePath) {
        return filePath.filter(path1 -> new File(path1).exists()).isPresent();
    }

    /**
     * Utility method to copy a resource from the jar to local file system
     * @param resourcePath resource path in the jar
     * @param destPath local file to copy to. this has to be a file
     * @throws IOException in case of error
     */
    public static void copyResourceAsFile(String resourcePath, String destPath) throws IOException {
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                        Utils.class.getResourceAsStream(resourcePath)));
                PrintWriter printWriter = new PrintWriter(new FileWriter(new File(destPath)))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                printWriter.println(line);
            }
        }
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
}
