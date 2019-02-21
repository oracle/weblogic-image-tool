package com.oracle.weblogicx.imagebuilder.api.model;

import com.oracle.weblogicx.imagebuilder.api.FileResolver;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Base class to represent either an installer or a patch file
 */
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
