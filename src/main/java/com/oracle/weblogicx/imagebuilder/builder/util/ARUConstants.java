/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.builder.util;

public class ARUConstants {

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

    private ARUConstants() {
        //restrict access
    }
}
