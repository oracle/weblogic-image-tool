// Copyright (c) 2021, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class BuilderTest {
    private static final String BUILD_CONTEXT = "/some/context";
    private static final String BUILD_ENGINE = "docker";

    private String expected(String options) {
        return String.format("%s buildx build %s %s --progress=plain", BUILD_ENGINE, options, BUILD_CONTEXT);
    }

    @Test
    void testSimpleCommand() {
        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT);
        cmd.tag("img:1");
        assertEquals(expected("--tag img:1"), cmd.toString());
    }

    @Test
    void testBuildWithOptions() {
        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT)
            .tag("img:2")
            .pull(true)
            .forceRm(true)
            .network("private-net")
            .buildArg("http_proxy", "http://blah/blah");
        assertEquals(
            expected("--tag img:2 --pull --force-rm --network private-net --build-arg http_proxy=http://blah/blah"),
            cmd.toString());
    }

    @Test
    void testBuildArgsInMap() {
        Map<String,String> argMap = new LinkedHashMap<>();
        argMap.put("something", "else");
        argMap.put("random", "data");

        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT)
            .tag("img:2")
            .pull(true)
            .forceRm(true)
            .network("private-net")
            .buildArg(argMap);
        assertEquals(
            expected("--tag img:2 --pull --force-rm --network private-net "
                + "--build-arg something=else --build-arg random=data"),
            cmd.toString());
    }

    @Test
    void testBuildArgsInMap2() {
        Map<String,String> argMap = null;

        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT)
            .tag("img:2")
            .pull(true)
            .forceRm(true)
            .network("private-net")
            .buildArg(argMap);
        assertEquals(
            expected("--tag img:2 --pull --force-rm --network private-net"),
            cmd.toString());
    }

    @Test
    void testBuildWithProxyPass() {
        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT);
        cmd.tag("img:3").buildArg("http_proxy", "http://user:pass@blah/blah", true);
        assertEquals(expected("--tag img:3 --build-arg http_proxy=********"), cmd.toString());
    }

    @Test
    void testBuildArgsWithPlatform() {
        Map<String,String> argMap = null;

        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT)
            .tag("img:4")
            .pull(true)
            .platform(new ArrayList<>(Collections.singletonList("linux/amd64")))
            .forceRm(true)
            .network("private-net")
            .buildArg(argMap);

        // expect that "--force-rm" will not be used, expect "buildx build" will be used
        assertEquals(
            String.format("%s buildx build %s %s %s",
                BUILD_ENGINE,
                "--tag img:4 --pull --platform linux/amd64 --force-rm --network private-net",
                BUILD_CONTEXT,
                "--progress=plain"),
            cmd.toString());
    }
}
