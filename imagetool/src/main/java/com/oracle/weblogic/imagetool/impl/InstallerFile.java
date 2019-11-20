// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.impl;

import java.util.Collections;
import java.util.List;

import com.oracle.weblogic.imagetool.api.meta.CacheStore;
import com.oracle.weblogic.imagetool.api.model.AbstractFile;
import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * This represents a WLS, JDK or WDT installer.
 */
public class InstallerFile extends AbstractFile {

    private InstallerType type;
    private static final LoggingFacade logger = LoggingFactory.getLogger(InstallerFile.class);

    public InstallerFile(InstallerType type, String version, String userId, String password) {
        super(type.toString(), version, userId, password);
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(CacheStore cacheStore) throws Exception {
        // check entry exists in cache
        String key = getKey();
        logger.entering(key);
        String filePath = cacheStore.getValueFromCache(key);
        if (!isFileOnDisk(filePath)) {
            throw new Exception(Utils.getMessage("IMG-0011", key));
        }
        logger.exiting(filePath);

        return filePath;
    }


    /**
     * Constructs the build-arg required to pass to the docker build.
     *
     * @param location path to installer on local disk
     * @return list of args
     */
    public List<String> getBuildArg(String location) {
        if (type != null) {
            return type.getBuildArg(location);
        }
        return Collections.emptyList();
    }
}
