/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                             
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.api.model;

import com.oracle.weblogic.imagetool.api.FileResolver;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Base class to represent either an installer or a patch file
 */
public abstract class AbstractFile implements FileResolver {

    private final Logger logger = Logger.getLogger(AbstractFile.class.getName());

    protected String key;
    protected CachePolicy cachePolicy;
    protected String userId;
    protected String password;

    public AbstractFile(String key, CachePolicy cachePolicy, String userId, String password) {
        this.key = key;
        this.cachePolicy = cachePolicy;
        this.userId = userId;
        this.password = password;
        logger.finer("Exiting AbstractFile construction: key=" + key);
    }

    protected boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    public String getKey() {
        return key;
    }
}
