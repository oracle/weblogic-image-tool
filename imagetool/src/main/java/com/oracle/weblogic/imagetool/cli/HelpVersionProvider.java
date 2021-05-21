// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

public class HelpVersionProvider implements IVersionProvider {

    private static final Properties projectProps = new Properties();

    /**
     * Get the project name and version declared in the POM.
     * @return project name and version number
     * @throws IOException if loading version properties fails.
     */
    public static Properties projectProperties() throws IOException {
        if (projectProps.isEmpty()) {
            try (InputStream versionInfoStream =
                     HelpVersionProvider.class.getResourceAsStream("/version-info.properties")) {
                if (versionInfoStream != null) {
                    projectProps.load(versionInfoStream);
                }
            }
        }
        return projectProps;
    }

    public static String versionString() throws IOException {
        return "WebLogic Image Tool version " + projectProperties().getProperty("project_version");
    }

    @Override
    public String[] getVersion() throws Exception {
        Properties p = projectProperties();
        String[] result = new String[1];
        result[0] = p.getProperty("project_name") + ":" + p.getProperty("project_version");
        return result;
    }
}
