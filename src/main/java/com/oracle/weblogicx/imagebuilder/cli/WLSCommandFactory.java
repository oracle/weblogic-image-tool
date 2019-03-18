/* Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved. 
*                                                              
* Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl. 
*/
package com.oracle.weblogicx.imagebuilder.cli;

import picocli.CommandLine.IFactory;

import java.lang.reflect.Constructor;

public class WLSCommandFactory implements IFactory {
    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            //try to find the one that takes boolean param
            Constructor<K> constructor = cls.getDeclaredConstructor(boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(true);
        } catch (Exception ex1) {
            try {
                return cls.newInstance();
            } catch (Exception ex2) {
                Constructor<K> constructor = cls.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
        }
    }
}
