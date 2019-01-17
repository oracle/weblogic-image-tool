package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.*;
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

    public static void main(String[] args) {
        System.out.println(doesFileExists(Optional.of("/hello")));
    }

}
