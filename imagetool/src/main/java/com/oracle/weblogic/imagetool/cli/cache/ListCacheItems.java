// Copyright (c) 2019, 2024, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.cache;

import java.util.Map;
import java.util.regex.Pattern;

import com.oracle.weblogic.imagetool.api.model.CommandResponse;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

@Command(
        name = "listItems",
        description = "List cache contents"
)
public class ListCacheItems extends CacheOperation {

    @Override
    public CommandResponse call() throws CacheStoreException {
        Map<String, String> resultMap = cache().getCacheItems();
        if (resultMap == null || resultMap.isEmpty()) {
            return CommandResponse.success("IMG-0047");
        } else {
            System.out.println("Cache contents");

            Pattern pattern = Pattern.compile(key == null ? ".*" : key);
            resultMap.entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).matches())
                .forEach(System.out::println);

            return CommandResponse.success(null);
        }
    }

    @Option(
        names = {"--key"},
        description = "list only cached items where the key matches this regex"
    )
    private String key;
}
