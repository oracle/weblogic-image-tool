// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Constants {

    static final String WDT_TAGS_URL = "https://api.github.com/repos/oracle/weblogic-deploy-tooling/tags";
    public static final String WDT_URL_FORMAT = "https://github.com/oracle/weblogic-deploy-tooling/releases/download/%s/weblogic-deploy.zip";
    static final String REL_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases";
    static final String LATEST_PSU_URL =
            "https://updates.oracle.com/Orion/Services/search?product=%s&release=%s";
    static final String ARU_LANG_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_languages";
    static final String PATCH_SEARCH_URL = "https://updates.oracle"
            + ".com/Orion/Services/search?product=%s&bug=%s&release=%s";
    static final String CONFLICTCHECKER_URL = "https://updates.oracle.com/Orion/Services/conflict_checks";
    static final String GET_LSINVENTORY_URL = "https://updates.oracle.com/Orion/Services/get_inventory_upi";
    static final String BUG_SEARCH_URL = "https://updates.oracle.com/Orion/Services/search?bug=%s";
    static final String WLS_PROD_ID = "15991";
    static final String FMW_PROD_ID = "27638";
    static final String OPATCH_PROD_ID = "31944";
    public static final String OPATCH_PATCH_TYPE = "opatch";
    public static final String CACHE_DIR_KEY = "cache.dir";
    public static final String DEFAULT_WLS_VERSION = "12.2.1.3.0";
    public static final String DEFAULT_JDK_VERSION = "8u202";
    public static final String WEBLOGIC_IMAGETOOL = "com/oracle/weblogicx/imagetool";
    public static final String METADATA_PREF_KEY = "metadata.file";
    public static final String DEFAULT_META_FILE = ".metadata";
    public static final String DELETE_ALL_FOR_SURE = "deleteAll4Sure";
    public static final String INSTALLER_WLS = "wls";
    public static final String INSTALLER_FMW = "fmw";
    public static final List<String> REQD_WDT_BUILD_ARGS = Stream.of(
            "DOMAIN_NAME", "ADMIN_NAME", "ADMIN_HOST", "ADMIN_PORT", "MANAGED_SERVER_PORT"
    ).collect(Collectors.toList());
    public static final String BUILD_ARG = "--build-arg";
    public static final String FILE_CACHE = "FILE";
    public static final String PREF_CACHE = "PREF";
    public static final String CACHE_STORE_TYPE = "cacheStoreType";
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String PATCH_ID_REGEX =  "^(\\d{8})(?:[_][0-9][0-9]\\.[0-9]\\.[0-9]\\.[0-9]\\.(\\d+))?";
    public static final String RIGID_PATCH_ID_REGEX =  "^(\\d{8})[_][0-9][0-9]\\.[0-9]\\.[0-9]\\.[0-9]\\.(\\d+)";
    //Option flags
    public static final String YUM = "_YUM";
    public static final String APTGET = "_APT";
    public static final String ZYPPER = "_SUSE";


    private Constants() {
        //restrict access
    }
}
