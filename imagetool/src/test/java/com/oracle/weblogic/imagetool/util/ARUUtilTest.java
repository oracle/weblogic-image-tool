// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.util;

import java.util.List;
import java.util.logging.Level;

import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(EasyMockExtension.class)
@Tag("unit")
public class ARUUtilTest {

    @Mock
    private SearchHelper mock;

    private static LoggingFacade logger = LoggingFactory.getLogger(ARUUtil.class);
    private static Level oldLevel;

    @BeforeAll
    public static void setUp() {
        oldLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
    }

    @AfterAll
    public static void tearDown() {
        logger.setLevel(oldLevel);
    }

    @Test
    public void testRecommendedPSUPatches() throws Exception {
        String[] expected = { "30965714_12.2.1.3.0","28512225_12.2.1.3.0","28278427_12.2.1.3.0" };
        expect(mock.getCategory()).andReturn(FmwInstallerType.WLS).anyTimes();
        expect(mock.getRelease()).andReturn(ARUUtilTestConstants.ReleaseNumber).anyTimes();
        expect(mock.getVersion()).andReturn(ARUUtilTestConstants.Version).anyTimes();
        mock.getXmlContent(anyString());
        expectLastCall().times(2);
        expect(mock.getResults()).andReturn(ARUUtilTestConstants.getReleasesResponse());
        mock.setRelease(ARUUtilTestConstants.ReleaseNumber);
        expectLastCall();
        expect(mock.isSuccess()).andReturn(true).anyTimes();
        expect(mock.getResults()).andReturn(ARUUtilTestConstants.getPatchesResponse());
        replay(mock);
        List<String> resultList =
            ARUUtil.getLatestPSURecommendedPatches(mock);
        verify(mock);
        assertNotNull(resultList);
        String[] resultArray = resultList.toArray(new String[resultList.size()]);
        assertArrayEquals(expected, resultArray);
    }

}