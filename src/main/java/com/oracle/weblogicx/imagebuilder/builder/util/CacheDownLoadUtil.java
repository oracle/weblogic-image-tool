package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CacheDownLoadUtil {
    public static String getDownloadRoot() {
        return "/tmp";
    }

    public static boolean existsInCache(String bugNumber, String fileName) {
        String fullPatchName = getDownloadRoot() + File.separator + fileName;
        Path path = Paths.get(fullPatchName);
        if (Files.exists(path))
            return true;
        else
            System.out.println("already downloaded");

        return false;
    }

    public static void updateTableOfContext(String bugNumber, String fileName) {}

}
