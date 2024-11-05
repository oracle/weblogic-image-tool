// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {

    public static final String ARU_UPDATES_HOST =
        Utils.getEnvironmentProperty("WLSIMG_ARU_HOST", () -> "updates.oracle.com");
    public static final String ARU_REST_URL = "https://" + ARU_UPDATES_HOST + "/Orion/Services";
    public static final String REL_URL = ARU_REST_URL + "/metadata?table=aru_releases";
    public static final String RECOMMENDED_PATCHES_URL = ARU_REST_URL
        + "/search?patch_type=all&life_cycle=Recommended&product=%s&release=%s";
    public static final String ARU_LANG_URL = ARU_REST_URL + "/metadata?table=aru_languages";
    public static final String CONFLICTCHECKER_URL = ARU_REST_URL + "/conflict_checks";
    public static final String CACHE_DIR_KEY = "cache.dir";
    public static final String DEFAULT_WLS_VERSION = "12.2.1.3.0";
    public static final String DEFAULT_JDK_VERSION = "8u202";
    public static final String DEFAULT_META_FILE = ".metadata";
    public static final String DELETE_ALL_FOR_SURE = "deleteAll4Sure";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String BUSYBOX = "busybox";
    public static final List<String> BUSYBOX_OS_IDS = Collections.unmodifiableList(Arrays.asList("bb", "alpine"));
    public static final String ORACLE_LINUX = "ghcr.io/oracle/oraclelinux:8-slim";
    public static final String BUILDER_DEFAULT = Utils.getEnvironmentProperty("WLSIMG_BUILDER", () -> "docker");
    public static final String CTX_JDK = "jdk/";
    public static final String CTX_FMW = "fmw/";
    public static final String AMD64_BLD = "linux/amd64";
    public static final String ARM64_BLD = "linux/arm64";

    private Constants() {
        //restrict access
    }
}
