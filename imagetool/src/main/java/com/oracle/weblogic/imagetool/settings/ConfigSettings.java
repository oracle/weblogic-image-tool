// Copyright (c) 2023, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.settings;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

/**
 * Wrapper class for UserSettings where default values are returned when no user setting exists for a given attribute.
 */
public class ConfigSettings {
    private static final LoggingFacade logger = LoggingFactory.getLogger(ConfigSettings.class);

    UserSettingsFile userSettings;

    /**
     * Parent directory for the build context directory.
     * A temporary folder created under "Build Directory" with the prefix "wlsimgbuilder_tempXXXXXXX" will be created
     * to hold the image build context (files, and Dockerfile).
     */
    public String imageBuildContextDirectory() {
        String result = userSettings.getBuildContextDirectory();
        if (Utils.isEmptyString(result)) {
            return ".";
        }
        return result;
    }

    /**
     * Patch download directory.
     * The directory for storing and using downloaded patches.
     */
    public String patchDirectory() {
        String result = userSettings.getPatchDirectory();
        if (Utils.isEmptyString(result)) {
            return UserSettingsFile.getSettingsDirectory().resolve("patches").toString();
        }
        return result;
    }

    /**
     * Installer download directory.
     * The directory for storing and using downloaded Java and middleware installers.
     */
    public String installerDirectory() {
        String result = userSettings.getInstallerDirectory();
        if (Utils.isEmptyString(result)) {
            return UserSettingsFile.getSettingsDirectory().resolve("installers").toString();
        }
        return result;
    }

    /**
     * Container image build tool.
     * Allow the user to specify the executable that will be used to build the container image.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String buildEngine() {
        String result = userSettings.getBuildEngine();
        if (Utils.isEmptyString(result)) {
            return "docker";
        }
        return result;
    }

    /**
     * Container image runtime tool.
     * Allow the user to specify the executable that will be used to run and/or interrogate images.  For example,
     * "/usr/local/bin/docker" or just "docker" if "docker" is on the user's path.  For example, "podman" or "docker".
     */
    public String containerEngine() {
        String result = userSettings.getContainerEngine();
        if (Utils.isEmptyString(result)) {
            return buildEngine();
        }
        return result;
    }

    /**
     * REST calls to ARU should be retried up to this number of times.
     */
    public Integer aruRetryMax() {
        Integer result = userSettings.getAruRetryMax();
        if (result == null) {
            return 10;
        }
        return result;
    }

    /**
     * The time between each ARU REST call in milliseconds.
     */
    public int aruRetryInterval() {
        Integer result = userSettings.getAruRetryInterval();
        if (result == null) {
            return 500;
        }
        return result;
    }
}
