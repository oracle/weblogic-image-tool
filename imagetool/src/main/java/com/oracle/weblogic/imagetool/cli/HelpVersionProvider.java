// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

public class HelpVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        List<String> retList = new ArrayList<>();
        try (InputStream versionInfoStream = this.getClass().getResourceAsStream("/version-info.properties")) {
            if (versionInfoStream != null) {
                Properties versionProps = new Properties();
                versionProps.load(versionInfoStream);
                retList.add(String.format("%s:%s", versionProps.getProperty("project_name", "imagetool"),
                        versionProps.getProperty("project_version", "0.0")));
            }
        }
        return retList.toArray(new String[0]);
    }
}
