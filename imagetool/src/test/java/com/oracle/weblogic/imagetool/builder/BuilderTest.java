// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.builder;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class BuilderTest {
    private static final String BUILD_CONTEXT = "/some/context";
    private static final String BUILD_ENGINE = "docker";

    private String expected(String options) {
        return String.format("%s build --no-cache %s %s", BUILD_ENGINE, options, BUILD_CONTEXT);
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
    void testBuildWithProxyPass() {
        BuildCommand cmd = new BuildCommand(BUILD_ENGINE, BUILD_CONTEXT);
        cmd.tag("img:3").buildArg("http_proxy", "http://user:pass@blah/blah", true);
        assertEquals(expected("--tag img:3 --build-arg http_proxy=********"), cmd.toString());
    }
}
