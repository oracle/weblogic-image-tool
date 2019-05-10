package com.oracle.weblogic.imagetool.cli;

import picocli.CommandLine.IVersionProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
