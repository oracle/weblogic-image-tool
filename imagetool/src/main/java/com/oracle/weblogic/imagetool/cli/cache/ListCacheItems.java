// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Collections;
import java.util.Map;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;

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
                String cacheDir = resultMap.getOrDefault(Constants.CACHE_DIR_KEY, null);
                if (!Utils.isEmptyString(cacheDir)) {
                    System.out.println(Constants.CACHE_DIR_KEY + "=" + cacheDir);
                    resultMap.remove(Constants.CACHE_DIR_KEY);
                }
                resultMap.forEach((key, value) -> System.out.println(key + "=" + value));
            }
            return new CommandResponse(0, "Cache contents", resultMap);
        }
    }

}
