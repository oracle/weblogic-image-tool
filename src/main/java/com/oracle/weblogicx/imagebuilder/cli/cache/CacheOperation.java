/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.cache;

import com.oracle.weblogicx.imagebuilder.api.meta.CacheStore;
import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.impl.meta.CacheStoreFactory;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

import java.util.List;
import java.util.concurrent.Callable;

public abstract class CacheOperation implements Callable<CommandResponse> {

//    @Option(
//            names = {"--cacheStoreType"},
//            description = "Whether to use file backed cache store or preferences backed cache store. Ex: file or pref",
//            hidden = true,
//            defaultValue = "file"
//    )
    protected CacheStore cacheStore = new CacheStoreFactory().get();

    @Unmatched
    List<String> unmatchedOptions;

    @Spec
    CommandSpec spec;
}
