// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.tests.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Class for executing shell commands from java.
 */
public class ExecCommand {

    public static ExecResult exec(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        Process p = null;

        try {
            pb.redirectErrorStream(true);
            Map<String,String> processEnv = pb.environment();
            processEnv.put("WLSIMG_BLDDIR", System.getProperty("WLSIMG_BLDDIR"));
            processEnv.put("WLSIMG_CACHEDIR", System.getProperty("WLSIMG_CACHEDIR"));

            p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), UTF_8));
            StringBuilder processOut = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                processOut.append(line);
                System.out.println(line);
            }

            p.waitFor();
            return new ExecResult(p.exitValue(), processOut.toString());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }
}
