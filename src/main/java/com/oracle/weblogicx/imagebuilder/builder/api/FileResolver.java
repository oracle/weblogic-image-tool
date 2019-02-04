package com.oracle.weblogicx.imagebuilder.builder.api;

import com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver;

public interface FileResolver {

    String resolve(MetaDataResolver metaDataResolver) throws Exception;

}
