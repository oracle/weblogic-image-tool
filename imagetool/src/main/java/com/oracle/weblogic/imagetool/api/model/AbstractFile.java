/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                             
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogic.imagetool.api.model;

import com.oracle.weblogic.imagetool.api.FileResolver;
import com.oracle.weblogic.imagetool.api.meta.CacheStore;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Base class to represent either an installer or a patch file
 */
public abstract class AbstractFile implements FileResolver {

    private String key;
    protected CachePolicy cachePolicy;
    protected String userId;
    protected String password;

    public AbstractFile(String id, String version, CachePolicy cachePolicy, String userId, String password) {
        this.key = generateKey(id, version);
        this.cachePolicy = cachePolicy;
        this.userId = userId;
        this.password = password;
    }

    public static String generateKey(String id, String version) {
        return id + CacheStore.CACHE_KEY_SEPARATOR + version;
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    public String getKey() {
        return key;
    }
}
