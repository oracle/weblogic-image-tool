package com.oracle.weblogicx.imagebuilder.builder.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Tester {

    public static void main(String[] args) throws IOException {
        Path DEFAULT_META_PATH = Paths.get(System.getProperty("user.home") + File.separator + "cache" +
                File.separator + ".metadata");
        System.out.println(DEFAULT_META_PATH.toString());
        System.out.println(DEFAULT_META_PATH.toAbsolutePath());

        System.out.println("============");
    }

}
