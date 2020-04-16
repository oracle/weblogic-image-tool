// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Map;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "listItems",
        description = "List cache contents"
)
public class ListCacheItems extends CacheOperation {

    @Override
    public CommandResponse call() {
        Map<String, String> resultMap = cache().getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            return new CommandResponse(0, "IMG-0047");
        } else {
            System.out.println("Cache contents");
            String cacheDir = resultMap.getOrDefault(Constants.CACHE_DIR_KEY, null);
            if (!Utils.isEmptyString(cacheDir)) {
                System.out.println(Constants.CACHE_DIR_KEY + "=" + cacheDir);
                resultMap.remove(Constants.CACHE_DIR_KEY);
            }

            Pattern pattern = Pattern.compile(key == null ? ".*" : key);
            resultMap.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .forEach(System.out::println);

            return new CommandResponse(0, "");
        }
    }

    @Option(
        names = {"--key"},
        description = "list only cached items where the key matches this regex"
    )
    private String key;
}
