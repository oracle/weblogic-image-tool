// Copyright (c) 2020, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Utils;

public class BuildCommand extends AbstractCommand {
    private static final LoggingFacade logger = LoggingFactory.getLogger(BuildCommand.class);

    private final String executable;
    private final List<String> command;
    private final List<BuildArg> buildArgs;
    private List<String> additionalOptions;
    private final String context;
    private boolean useBuildx = true;
    private List<String> buildPlatforms = new ArrayList<>();

    /**
     * Create a build command for creating an image.  At some point, it might
     * be beneficial to subclass this with separate classes for each builder (docker or podman).
     * For now, the differences do not justify the extra complexity.
     */
    public BuildCommand(String buildEngine, String contextFolder) {
        Objects.requireNonNull(contextFolder);
        buildArgs = new ArrayList<>();
        executable = buildEngine;
        command = new ArrayList<>();
        context = contextFolder;
    }

    /**
     * Add Docker image tag name for this build command.
     * @param value name to be used as the image tag.
     * @return this
     */
    public BuildCommand tag(String value) {
        if (Utils.isEmptyString(value)) {
            return this;
        }
        command.add("--tag");
        command.add(value);
        return this;
    }

    /**
     * Toggle the use of the BuildKit.
     * If true, the build command will start "buildx build".
     * If false, the build command will start "build".
     * @param value true to enable buildx
     * @return this
     */
    public BuildCommand useBuildx(boolean value) {
        useBuildx = value;
        return this;
    }

    /**
     * Add container build platform.  Pass the desired
     * build architecture to the build process.
     * @param value a single platform name.
     * @return this
     */
    public BuildCommand platform(List<String> value) {
        buildPlatforms = value;
        if (value == null || value.isEmpty()) {
            return this;
        }
        command.add("--platform");
        command.add(String.join(",", value));
        // Only use buildx for multi platform build
        if (value.size() > 1) {
            useBuildx = true;
        }
        return this;
    }

    /**
     * Always remove intermediate containers if set to true.
     * By default, Docker leaves intermediate containers when the build fails which is not ideal for CI/CD servers.
     * @param value true to enable --force-rm on docker build.
     * @return this
     */
    public BuildCommand forceRm(boolean value) {
        if (value && useBuildx) {
            command.add("--force-rm");
        }
        return this;
    }

    public BuildCommand additionalOptions(List<String> options) {
        additionalOptions = options;
        return this;
    }

    /**
     * Add a --build-arg to the Docker build command.
     * Conceal is defaulted to false.
     * @param key the ARG
     * @param value the value to be used in the Dockerfile for this ARG
     */
    public BuildCommand buildArg(String key, String value) {
        return buildArg(key, value, false);
    }

    /**
     * Add a --build-arg to the Docker build command.
     * @param key the ARG
     * @param value the value to be used in the Dockerfile for this ARG
     * @param conceal true for passwords so the value is not logged
     */
    public BuildCommand buildArg(String key, String value, boolean conceal) {
        if (Utils.isEmptyString(value)) {
            return this;
        }
        BuildArg arg = new BuildArg();
        arg.key = key;
        arg.value = value;
        arg.conceal = conceal;
        buildArgs.add(arg);
        return this;
    }

    /**
     * Add multiple --build-arg key value pairs to the Docker build command.
     * @param keyValuePairs A map of key-value pairs to be used directly with the container image builder
     */
    public BuildCommand buildArg(Map<String,String> keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.isEmpty()) {
            return this;
        }

        for (Map.Entry<String,String> entry : keyValuePairs.entrySet()) {
            buildArg(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Add a --network to the Docker build command.
     * @param value the Docker network to use
     */
    public BuildCommand network(String value) {
        if (Utils.isEmptyString(value)) {
            return this;
        }
        command.add("--network");
        command.add(value);
        return this;
    }

    /**
     * Add a --pull to the Docker build command.  If value is false, return without adding the --pull.
     * @param value true to add the pull
     */
    public BuildCommand pull(boolean value) {
        if (value) {
            command.add("--pull");
        }
        return this;
    }

    /**
     * Add a --push to the Docker build command.  If value is false, return without adding the --push.
     * @param value true to add the pull
     */
    public BuildCommand push(boolean value) {
        if (value) {
            command.add("--push");
        }
        return this;
    }

    /**
     * Add a --load to the Docker build command.  If value is false, return without adding the --load.
     * @param value true to add the pull
     */
    public BuildCommand load(boolean value) {
        if (value) {
            command.add("--load");
        }
        return this;
    }


    @Override
    public List<String> getCommand(boolean showPasswords) {
        List<String> result = new ArrayList<>();
        result.add(executable);
        if (useBuildx) {
            result.add("buildx");
        }
        result.add("build");
        result.addAll(command);
        if (additionalOptions != null && !additionalOptions.isEmpty()) {
            result.addAll(additionalOptions);
        }
        for (BuildArg arg : buildArgs) {
            result.addAll(arg.toList(showPasswords));
        }
        result.add(context);
        result.add("--progress=plain");

        return result;
    }

    /**
     * Return the build engine used.
     * @return build engine passed or used
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * Return the build platforms passed in.
     * @return build platforms
     */

    public List<String> getBuildPlatforms() {
        return buildPlatforms;
    }

    /**
     * Substitute the platform value.
     * @param platform platform value used for substitution
     */
    public void substitutePlatform(String platform) {
        for (int i = 0; i < command.size() - 1; i++) {
            if (command.get(i).equals("--platform")) {
                command.set(i + 1, platform);
            }
        }
    }

    /**
     * Substitute the tagname value.
     * @param tagName platform value used for substitution
     */
    public String substituteTagName(String tagName) {
        for (int i = 0; i < command.size() - 1; i++) {
            if (command.get(i).equals("--tag")) {
                command.set(i + 1, tagName);
                return command.get(i + 1);
            }
        }
        return null;
    }

    /**
     * Return the tag name value.
     * @return tag name
     */
    public String getTagName() {
        for (int i = 0; i < command.size() - 1; i++) {
            if (command.get(i).equals("--tag")) {
                return command.get(i + 1);
            }
        }
        return null;
    }

    /**
     * Return context folder.
     * @return context folder
     */
    public String getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.join(" ", getCommand(false));
    }

    private static class BuildArg {
        String key;
        String value;
        boolean conceal;

        List<String> toList(boolean reveal) {
            List<String> result = new ArrayList<>();
            result.add("--build-arg");
            if (conceal && !reveal) {
                result.add(key + "=" + "********");
            } else {
                result.add(key + "=" + value);
            }
            return result;
        }
    }
}
