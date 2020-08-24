// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

public final class Constants {

    public static final String REL_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases";
    public static final String RECOMMENDED_PATCHES_URL = "https://updates.oracle.com/Orion/Services/search?patch_type=all&life_cycle=Recommended&product=%s&release=%s";
    public static final String ARU_LANG_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_languages";
    public static final String CONFLICTCHECKER_URL = "https://updates.oracle.com/Orion/Services/conflict_checks";
    public static final String GET_LSINVENTORY_URL = "https://updates.oracle.com/Orion/Services/get_inventory_upi";
    public static final String CACHE_DIR_KEY = "cache.dir";
    public static final String DEFAULT_WLS_VERSION = "12.2.1.3.0";
    public static final String DEFAULT_JDK_VERSION = "8u202";
    public static final String DEFAULT_META_FILE = ".metadata";
    public static final String DELETE_ALL_FOR_SURE = "deleteAll4Sure";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String PATCH_ID_REGEX =  "^(\\d{8})(?:[_][0-9][0-9]\\.[0-9]\\.[0-9]\\.[0-9]\\.(\\d+))?";
    public static final String RIGID_PATCH_ID_REGEX =  "^(\\d{8})[_][0-9][0-9]\\.[0-9]\\.[0-9]\\.[0-9]\\.(\\d+)";


    private Constants() {
        //restrict access
    }
}
