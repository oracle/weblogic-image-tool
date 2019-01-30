/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.util;

public final class ARUConstants {

    public static final String OPATCH_1394_KEY = "opatch_1394";
    public static final String OPATCH_1394_URL="https://updates.oracle.com/Orion/Services/download/" +
            "p28186730_139400_Generic.zip?aru=22310944&patch_file=p28186730_139400_Generic.zip";
    static final String REL_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases";
    static final String LATEST_PSU_URL =
            "https://updates.oracle.com/Orion/Services/search?product=%s&release=%s";
    static final String ARU_LANG_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_languages";
    static final String PATCH_SEARCH_URL="https://updates.oracle"
        + ".com/Orion/Services/search?product=%s&bug=%s&release=%s";
    static final String CONFLICTCHECKER_URL = "https://updates.oracle.com/Orion/Services/conflict_checks";
    static final String WLS_PROD_ID = "15991";
    static final String FMW_PROD_ID = "27638";
    public static final String CACHE_KEY_SEPARATOR = "_";
    public static final String CACHE_DIR_KEY = "cache.dir";
    public static final String DEFAULT_WLS_VERSION = "12.2.1.3.0";
    public static final String DEFAULT_JDK_VERSION = "8u201";
    public static final String WEBLOGICX_IMAGEBUILDER = "com/oracle/weblogicx/imagebuilder";
    public static final String METADATA_PREF_KEY = "metadata.file";
    public static final String DEFAULT_META_FILE = ".metadata";
    public static final String CLI_OPTION = "--cli";

    private ARUConstants() {
        //restrict access
    }
}
