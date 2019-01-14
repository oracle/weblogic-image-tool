package com.oracle.weblogicx.imagebuilder.builder.util;

class ARUConstants {

    static final String REL_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_releases";
    static final String LATEST_PSU_URL =
            "https://updates.oracle.com/Orion/Services/search?product=%s&release=%s";
    static final String ARU_LANG_URL = "https://updates.oracle.com/Orion/Services/metadata?table=aru_languages";
    static final String PATCH_SEARCH_URL="https://updates.oracle.com/Orion/Services/search?product=%s&bug=%s";

    static final String WLS_PROD_ID = "15991";
    static final String FMW_PROD_ID = "27638";

    private ARUConstants() {
        //restrict access
    }
}
