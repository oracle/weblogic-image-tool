package com.oracle.weblogicx.imagebuilder.builder.api.model;

import com.oracle.weblogicx.imagebuilder.builder.api.FileResolver;

import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class AbstractFile implements FileResolver {

    protected String key;

    public AbstractFile(String key) {
        this.key = key;
    }

    protected boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    public String getKey() {
        return key;
    }
}
