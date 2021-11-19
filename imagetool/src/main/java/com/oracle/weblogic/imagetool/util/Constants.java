// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

public final class Constants {

    public static final String ARU_REST_URL = "https://updates.oracle.com/Orion/Services";
    public static final String REL_URL = ARU_REST_URL + "/metadata?table=aru_releases";
    public static final String RECOMMENDED_PATCHES_URL = ARU_REST_URL
        + "/search?patch_type=all&life_cycle=Recommended&product=%s&release=%s";
    public static final String ARU_LANG_URL = ARU_REST_URL + "/metadata?table=aru_languages";
    public static final String CONFLICTCHECKER_URL = ARU_REST_URL + "/conflict_checks";
    public static final String GET_LSINVENTORY_URL = ARU_REST_URL + "/get_inventory_upi";
    public static final String CACHE_DIR_KEY = "cache.dir";
    public static final String DEFAULT_WLS_VERSION = "12.2.1.3.0";
    public static final String DEFAULT_JDK_VERSION = "8u202";
    public static final String DEFAULT_META_FILE = ".metadata";
    public static final String DELETE_ALL_FOR_SURE = "deleteAll4Sure";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String PATCH_ID_REGEX =  "^(\\d{8})(?:[_][0-9][0-9](?:\\.[0-9]){3,8}\\.(\\d+))?";
    public static final String RIGID_PATCH_ID_REGEX =  "^(\\d{8})[_][0-9][0-9](?:\\.[0-9]){3,8}\\.(\\d+)";
    public static final String BUSYBOX = "busybox";

    private Constants() {
        //restrict access
    }
}
