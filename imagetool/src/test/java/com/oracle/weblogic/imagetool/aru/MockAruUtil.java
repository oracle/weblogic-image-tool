// Copyright (c) 2022, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.ResourceUtils;
import org.w3c.dom.Document;

public class MockAruUtil extends AruUtil {
    /**
     * Intercept calls to the ARU REST API during unit testing.
     */

    @Override
    Document getAllReleases(String userId, String password) {
        try {
            return ResourceUtils.getXmlFromResource("/releases.xml");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("failed to load releases.xml from resources", e);
        }
    }

    @Override
    public Stream<AruPatch> getPatches(String bugNumber, String user, String password)
        throws IOException, XPathExpressionException {
        if (user == null || password == null) {
            // running in offline mode (no credentials to connect to ARU)
            return Stream.of(new AruPatch().patchId(bugNumber));
        } else {
            return AruPatch.getPatches(
                ResourceUtils.getXmlFromResource("/patches/patch-" + bugNumber + ".xml"));
        }
    }

    public static void insertMockAruInstance(AruUtil mockInstance) throws NoSuchFieldException, IllegalAccessException {
        // insert test class into AruUtil to intercept REST calls to ARU
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, mockInstance);
    }

    public static void removeMockAruInstance() throws NoSuchFieldException, IllegalAccessException {
        // remove test class from AruUtil instance
        Field aruRest = AruUtil.class.getDeclaredField("instance");
        aruRest.setAccessible(true);
        aruRest.set(aruRest, null);
    }


}
