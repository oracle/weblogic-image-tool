// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.api.model;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.oracle.weblogic.imagetool.api.FileResolver;
import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;

/**
 * Base class to represent either an installer or a patch file.
 */
public abstract class AbstractFile implements FileResolver {

    private static final LoggingFacade logger = LoggingFactory.getLogger(AbstractFile.class);

    private String key;
    protected String userId;
    protected String password;


    /**
     * Construct a new abstract file.
     *
     * @param id          cache ID
     * @param version     applicable product version.
     * @param userId      userid to use for ARU.
     * @param password    password to use for ARU.
     */
    public AbstractFile(String id, String version,  String userId, String password) {
        this.key = generateKey(id, version);
        this.userId = userId;
        this.password = password;
    }

    public String generateKey(String id, String version) {

        logger.entering(id, version);
        // Note:  this will be invoked by InstallerFile or PatchFile
        //  for patches there are specific format, currently installers are
        //  wls, fmw, jdk.
        //
        //
        String mykey = id;
        if (id == null) {  // should never happens
            mykey = id + CacheStore.CACHE_KEY_SEPARATOR + version;
        } else if (id.indexOf('_') < 0) {
            if (version != null) {
                mykey = mykey + CacheStore.CACHE_KEY_SEPARATOR + version;
            }
        }

        logger.exiting(mykey);
        return mykey;
    }

    public static boolean isFileOnDisk(String filePath) {
        return filePath != null && Files.isRegularFile(Paths.get(filePath));
    }

    public String getKey() {
        return key;
    }
}
