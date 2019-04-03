/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import picocli.CommandLine.Command;

import java.util.Collections;
import java.util.Map;

import static com.oracle.weblogicx.imagebuilder.util.Constants.CACHE_DIR_KEY;

@Command(
        name = "listItems",
        description = "List cache contents"
)
public class ListCacheItems extends CacheOperation {

    public ListCacheItems() {
    }

    public ListCacheItems(boolean isCLIMode) {
        super(isCLIMode);
    }

    @Override
    public CommandResponse call() {
        Map<String, String> resultMap = cacheStore.getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            if (isCLIMode) {
                System.out.println("cache is empty");
            }
            return new CommandResponse(0, "cache is empty", Collections.<String, String>emptyMap());
        } else {
            if (isCLIMode) {
                System.out.println("Cache contents");
                String cacheDir = resultMap.getOrDefault(CACHE_DIR_KEY, null);
                if (!Utils.isEmptyString(cacheDir)) {
                    System.out.println(CACHE_DIR_KEY + "=" + cacheDir);
                    resultMap.remove(CACHE_DIR_KEY);
                }
                resultMap.forEach((key, value) -> System.out.println(key + "=" + value));
            }
            return new CommandResponse(0, "Cache contents", resultMap);
        }
    }

//    @Option(
//            names = {"--cacheStoreType"},
//            description = "Whether to use file backed cache store or preferences backed cache store. Ex: file or pref",
//            hidden = true,
//            defaultValue = "file"
//    )
//    private CacheStore cacheStore;
//
//    @Unmatched
//    List<String> unmatchedOptions;
}
