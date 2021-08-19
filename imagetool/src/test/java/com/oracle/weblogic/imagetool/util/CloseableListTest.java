// Copyright (c) 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CloseableListTest {

    // simple object used to test the CloseableList
    static final class MyStream implements Closeable {
        private boolean open;

        MyStream() {
            open = true;
        }

        @Override
        public void close() {
            open = false;
        }

        boolean isOpen() {
            return open;
        }
    }

    @Test
    void testAutoCloseObjects() {
        // create several closable objects (like a PrintStream...)
        MyStream[] someStreams = { new MyStream(), new MyStream()};
        // wrap with a list, although it shouldn't matter
        List<MyStream> originalList = new ArrayList<>(Arrays.asList(someStreams));
        // try-with-resource should close the outer list
        try (CloseableList<MyStream> testList = new CloseableList<>(originalList)) {
            // ensure that the inner objects are still "open"
            testList.forEach(x -> assertTrue(x.isOpen()));
        }
        // if the CloseableList class works correctly, the inner objects should all get closed when
        // the outer list is closed.
        originalList.forEach(x -> assertFalse(x.isOpen()));
    }
}
