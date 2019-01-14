package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.util.Optional;

public class Utils {

    public static boolean doesFileExists(Optional<String> filePath) {
        return filePath.filter(path1 -> new File(path1).exists()).isPresent();
    }

    public static void main(String[] args) {
        System.out.println(doesFileExists(Optional.of("/hello")));
    }
}
