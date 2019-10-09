// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli;

import java.lang.reflect.Constructor;

import picocli.CommandLine.IFactory;

public class WLSCommandFactory implements IFactory {
    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return cls.newInstance();
        } catch (Exception ex2) {
            Constructor<K> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
    }
}
