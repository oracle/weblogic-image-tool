// Copyright (c) 2026, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPathExpressionException;

import com.oracle.weblogic.imagetool.aru.AruException;
import com.oracle.weblogic.imagetool.aru.AruPatch;
import com.oracle.weblogic.imagetool.aru.AruUtil;
import com.oracle.weblogic.imagetool.aru.InstalledPatch;
import com.oracle.weblogic.imagetool.aru.MockAruUtil;
import com.oracle.weblogic.imagetool.aru.NoPatchesFoundException;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreTestImpl;
import com.oracle.weblogic.imagetool.installer.FmwInstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Architecture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CachedUnknownPatchTest {
    private static final String INSTALLER_VERSION = "12.2.1.4.0";
    private static final String PSU_VERSION = "12.2.1.4.250101";
    private static final String RECOMMENDED_PATCH = "11111111";
    private static final String KNOWN_PATCH = "88888888";
    private static final String UNKNOWN_PATCH = "99999999";

    @TempDir
    Path tempDir;

    private MatchingCacheStore cacheStore;
    private CachedPatchAruUtil aruUtil;

    @BeforeEach
    void setUp() throws Exception {
        cacheStore = new MatchingCacheStore(tempDir);
        setCacheStore(cacheStore);
        aruUtil = new CachedPatchAruUtil();
        MockAruUtil.insertMockAruInstance(aruUtil);
    }

    @AfterEach
    void tearDown() throws Exception {
        MockAruUtil.removeMockAruInstance();
        setCacheStore(null);
    }

    @Test
    void shouldApplyCachedPatchWhenAruDoesNotKnowPatch() throws Exception {
        Path recommendedFile = addPatchToCache(RECOMMENDED_PATCH, "recommended.zip");
        Path knownFile = addPatchToCache(KNOWN_PATCH, "known.zip");
        Path unknownFile = addPatchToCache(UNKNOWN_PATCH, "unknown.zip");
        Path buildDir = Files.createDirectory(tempDir.resolve("build"));

        CreateImage createImage = createImage(buildDir);
        RecordingHandler handler = new RecordingHandler();
        Logger rootLogger = Logger.getLogger("");
        LoggingFacade imageToolLogger = LoggingFactory.getLogger(CommonPatchingOptions.class);
        Level originalLevel = imageToolLogger.getLevel();
        rootLogger.addHandler(handler);
        imageToolLogger.setLevel(Level.ALL);
        try {
            createImage.initializeOptions();
            createImage.handlePatchFiles(Collections.emptyList());
        } finally {
            imageToolLogger.setLevel(originalLevel);
            rootLogger.removeHandler(handler);
        }

        assertEquals(Arrays.asList(RECOMMENDED_PATCH, KNOWN_PATCH), aruUtil.validatedPatchIds());
        assertTrue(Files.exists(buildDir.resolve("patches").resolve(recommendedFile.getFileName())));
        assertTrue(Files.exists(buildDir.resolve("patches").resolve(knownFile.getFileName())));
        assertTrue(Files.exists(buildDir.resolve("patches").resolve(unknownFile.getFileName())));
        assertTrue(createImage.dockerfileOptions.isPatchingEnabled());
        assertEquals(Arrays.asList("recommended.zip", "known.zip", "unknown.zip"),
            createImage.dockerfileOptions.patches());
        assertTrue(handler.containsWarning("IMG-0124", UNKNOWN_PATCH));
    }

    @Test
    void shouldFailWhenPatchIsMissingFromAruAndCache() throws Exception {
        Path buildDir = Files.createDirectory(tempDir.resolve("build"));
        CreateImage createImage = createImage(buildDir);
        createImage.initializeOptions();

        assertThrows(NoPatchesFoundException.class,
            () -> createImage.handlePatchFiles(Collections.emptyList()));
        assertFalse(aruUtil.validationCalled);
        assertFalse(Files.exists(buildDir.resolve("patches")));
    }

    @Test
    void shouldFailWhenCacheContainsOnlyPatchWithMatchingPrefix() throws Exception {
        addPatchToCache(UNKNOWN_PATCH + "0", "prefix-match.zip");
        Path buildDir = Files.createDirectory(tempDir.resolve("build"));
        CreateImage createImage = createImage(buildDir);
        createImage.initializeOptions();

        assertThrows(NoPatchesFoundException.class,
            () -> createImage.handlePatchFiles(Collections.emptyList()));
        assertFalse(aruUtil.validationCalled);
        assertFalse(Files.exists(buildDir.resolve("patches")));
    }

    private CreateImage createImage(Path buildDir) {
        CreateImage createImage = new CreateImage();
        new CommandLine(createImage).parseArgs("--tag", "test:latest", "--version", INSTALLER_VERSION,
            "--platform", "linux/amd64", "--user", "user", "--password", "password",
            "--recommendedPatches", "--patches", KNOWN_PATCH + "," + UNKNOWN_PATCH);
        createImage.setBuildDirectory(buildDir.toString());
        return createImage;
    }

    private Path addPatchToCache(String patchId, String fileName) throws IOException {
        Path patchFile = Files.write(tempDir.resolve(fileName), Collections.singletonList(patchId));
        cacheStore.addToCache(patchId + "_" + PSU_VERSION, patchFile.toString());
        return patchFile;
    }

    private static void setCacheStore(CacheStore value) throws Exception {
        Field store = CacheStoreFactory.class.getDeclaredField("store");
        store.setAccessible(true);
        store.set(null, value);
    }

    private static class MatchingCacheStore extends CacheStoreTestImpl {
        MatchingCacheStore(Path cacheDir) {
            super(cacheDir);
        }

        @Override
        public List<String> getKeysForType(String type) {
            return getCacheItems().keySet().stream()
                .filter(key -> key.startsWith(type))
                .collect(Collectors.toList());
        }
    }

    private static class CachedPatchAruUtil extends AruUtil {
        private List<AruPatch> validatedPatches = Collections.emptyList();
        private boolean validationCalled;

        @Override
        public boolean checkCredentials(String username, String password) {
            return true;
        }

        @Override
        public List<AruPatch> getRecommendedPatches(FmwInstallerType type, String version,
                                                    Architecture architecture, String userId, String password) {
            List<AruPatch> patches = new ArrayList<>();
            patches.add(patch(RECOMMENDED_PATCH).product("15991")
                .psuBundle("Oracle WebLogic Server " + PSU_VERSION));
            return patches;
        }

        @Override
        public Stream<AruPatch> getPatches(String bugNumber, String userId, String password)
            throws AruException, IOException, XPathExpressionException {
            if (userId == null || password == null) {
                return super.getPatches(bugNumber, userId, password);
            }
            if (UNKNOWN_PATCH.equals(bugNumber)) {
                throw new NoPatchesFoundException("Patch is not available from ARU");
            }
            if (KNOWN_PATCH.equals(bugNumber)) {
                return Stream.of(patch(KNOWN_PATCH));
            }
            return Stream.empty();
        }

        @Override
        public void validatePatches(List<InstalledPatch> installedPatches, List<AruPatch> patches,
                                    String userId, String password) {
            validationCalled = true;
            validatedPatches = new ArrayList<>(patches);
        }

        List<String> validatedPatchIds() {
            return validatedPatches.stream().map(AruPatch::patchId).collect(Collectors.toList());
        }

        private static AruPatch patch(String patchId) {
            return new AruPatch().patchId(patchId).version(PSU_VERSION).description("test patch")
                .release("123456").platform("2000");
        }
    }

    private static class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean containsWarning(String message, String parameter) {
            return records.stream().anyMatch(record -> Level.WARNING.equals(record.getLevel())
                && message.equals(record.getMessage()) && record.getParameters() != null
                && record.getParameters().length == 1 && parameter.equals(record.getParameters()[0]));
        }
    }
}
