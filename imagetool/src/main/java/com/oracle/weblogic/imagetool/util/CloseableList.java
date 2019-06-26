// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class CloseableList<E extends Closeable> extends ArrayList<E> implements Closeable {

    public CloseableList(int initialCapacity) {
        super(initialCapacity);
    }

    public CloseableList() {
        super();
    }

    public CloseableList(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public void close() {
        this.forEach(x -> {
            try {
                x.close();
            } catch (IOException e) {
                //suppress exception
            }
        });
    }
}
