// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.aru;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EasyMockExtension.class)
@Tag("unit")
class AruUtilTest {

    @Mock
    private AruHttpHelper mock;

    private static LoggingFacade logger = LoggingFactory.getLogger(AruUtil.class);
    private static Level oldLevel;

    @BeforeAll
    static void setUp() {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.SEVERE);

    }

    @AfterAll
    static void tearDown() {
        logger.setLevel(oldLevel);
    }

    @AfterEach
    void clearStatic() throws Exception {
        Field documentField = AruUtil.class.getDeclaredField("allReleasesDocument");
        documentField.setAccessible(true);
        documentField.set(null, null);
    }
    
    @Test
    void testRecommendedPatches() throws Exception {
        expect(mock.release()).andReturn(AruUtilTestConstants.ReleaseNumber).anyTimes();
        expect(mock.product()).andReturn(AruProduct.WLS).anyTimes();
        expect(mock.version()).andReturn(AruUtilTestConstants.Version).anyTimes();
        expect(mock.execSearch(anyString())).andReturn(mock).times(2);
        expect(mock.results()).andReturn(AruUtilTestConstants.getReleasesResponse());
        expect(mock.release(AruUtilTestConstants.ReleaseNumber)).andReturn(mock);
        expect(mock.success()).andReturn(true).anyTimes();
        expect(mock.results()).andReturn(AruUtilTestConstants.getPatchesResponse());
        replay(mock);
        List<AruPatch> resultList = AruUtil.getRecommendedPatches(mock);
        verify(mock);
        assertNotNull(resultList);
        AruPatch[] resultArray = resultList.toArray(new AruPatch[0]);
        String[] expected = {"30965714","28512225","28278427"};
        assertEquals(resultArray.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], resultArray[i].patchId());
        }
    }

    @Test
    void testNoRecommendedPatches() throws Exception {
        expect(mock.release()).andReturn(AruUtilTestConstants.ReleaseNumber).anyTimes();
        expect(mock.product()).andReturn(AruProduct.WLS).anyTimes();
        expect(mock.version()).andReturn(AruUtilTestConstants.Version).anyTimes();
        expect(mock.execSearch(anyString())).andReturn(mock).times(2);
        expect(mock.results()).andReturn(AruUtilTestConstants.getReleasesResponse());
        expect(mock.release(AruUtilTestConstants.ReleaseNumber)).andReturn(mock);
        expect(mock.success()).andReturn(true);
        expect(mock.success()).andReturn(false);
        expect(mock.errorMessage()).andReturn("No results found").anyTimes();
        replay(mock);
        List<AruPatch> resultList = AruUtil.getRecommendedPatches(mock);
        verify(mock);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void testRecommendedPsu() throws Exception {
        expect(mock.release()).andReturn(AruUtilTestConstants.ReleaseNumber).anyTimes();
        expect(mock.product()).andReturn(AruProduct.WLS).anyTimes();
        expect(mock.version()).andReturn(AruUtilTestConstants.Version).anyTimes();
        expect(mock.execSearch(anyString())).andReturn(mock).times(2);
        expect(mock.release(AruUtilTestConstants.ReleaseNumber)).andReturn(mock);
        expect(mock.results()).andReturn(AruUtilTestConstants.getReleasesResponse());
        expect(mock.success()).andReturn(true).anyTimes();
        expect(mock.results()).andReturn(AruUtilTestConstants.getPatchesResponse());
        replay(mock);
        List<AruPatch> result = AruUtil.getLatestPsu(mock);
        verify(mock);
        assertEquals("30965714", result.get(0).patchId());
    }

}