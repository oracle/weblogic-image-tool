// Copyright (c) 2020, Oracle Corporation and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("unit")
public class CommonOptionsTest {
    /**
     * Test CommonOptions handleChown method.
     * @throws Exception if reflection fails (developer error)
     */
    @Test
    void handleChown() throws Exception {
        CreateImage createImage = new CreateImage();

        // accessing private fields normally set by the command line
        Field optionsField = CommonOptions.class.getDeclaredField("dockerfileOptions");
        optionsField.setAccessible(true);
        DockerfileOptions dockerfile = new DockerfileOptions("testbuildid");
        optionsField.set(createImage, dockerfile);

        Field userAndGroupField = CommonOptions.class.getDeclaredField("osUserAndGroup");
        userAndGroupField.setAccessible(true);
        String[] userAndGroup = {"derek", "data"};
        userAndGroupField.set(createImage, userAndGroup);

        Method chownMethod = CommonOptions.class.getDeclaredMethod("handleChown");
        chownMethod.setAccessible(true);
        chownMethod.invoke(createImage);

        assertEquals("derek", dockerfile.userid(), "CommonOptions handleChown failed to process user");
        assertEquals("data", dockerfile.groupid(), "CommonOptions handleChown failed to process group");
    }

    /**
     * Test CommonOptions handleChown method.
     * @throws Exception if reflection fails (developer error)
     */
    @Test
    void handleAdditionalBuildCommands(@TempDir File buildDir) throws Exception {
        CreateImage createImage = new CreateImage();
        createImage.setTempDirectory(buildDir.getAbsolutePath());

        // accessing private fields normally set by the command line
        Field optionsField = CommonOptions.class.getDeclaredField("dockerfileOptions");
        optionsField.setAccessible(true);
        DockerfileOptions dockerfile = new DockerfileOptions("testbuildid");
        optionsField.set(createImage, dockerfile);

        Field commandsField = CommonOptions.class.getDeclaredField("additionalBuildCommandsPath");
        commandsField.setAccessible(true);
        Path additionalBuildCommandsFile = Paths.get("./src/test/resources/additionalBuildCommands/two-sections.txt");
        commandsField.set(createImage, additionalBuildCommandsFile);

        Field buildFiles = CommonOptions.class.getDeclaredField("additionalBuildFiles");
        buildFiles.setAccessible(true);
        List<Path> buildFilesList = new ArrayList<>();
        buildFilesList.add(Paths.get("./src/test/resources/buildFilesDir"));
        buildFiles.set(createImage, buildFilesList);

        // disregard INFO level messages for the test
        LoggingFacade logger = LoggingFactory.getLogger(CachedFile.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
        try {
            Method chownMethod = CommonOptions.class.getDeclaredMethod("handleAdditionalBuildCommands");
            chownMethod.setAccessible(true);
            chownMethod.invoke(createImage);
        } finally {
            logger.setLevel(oldLevel);
        }

        List<String> additionalCommands = dockerfile.beforeJdkInstall();
        assertEquals(2, additionalCommands.size(), "There should be 2 additional build commands");
        assertEquals("RUN ls -al /u01", additionalCommands.get(1),
            "The 2nd additional command that was loaded did not match expected value.");


        // get a list of files that was created in the build directory
        try (Stream<Path> walk = Files.walk(Paths.get(buildDir + "/files"))) {
            List<String> fileList = walk.filter(Files::isRegularFile)
                .map(Path::toString).collect(Collectors.toList());

            // should be 4 files, fileA, fileB, fileC, and fileD
            assertEquals(4, fileList.size(), "Should have copied 4 files: " + fileList);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        }

        // get a list of files that was created in the build directory
        try (Stream<Path> walk = Files.walk(Paths.get(buildDir + "/files"))) {
            List<String> dirList = walk.filter(Files::isDirectory)
                .map(Path::toString).collect(Collectors.toList());

            // should be 4 directories, files, buildFilesDir, subDirectory, and secondLevelDirectory
            assertEquals(4, dirList.size(), "There should be 4 directories: " + dirList);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * Test resolving options in a list of input files.
     *
     * @throws Exception if in error or IOException
     */
    @Test
    void testResolveOptions() throws Exception {
        final String IMG_NAME = "mydomain:latest";
        final String DOMAIN_HOME = "/u01/domains/xxxx";
        final String DOMAIN_SOURCE = "PersistentVolume";

        WdtOptions wdtOptions = new WdtOptions();

        // accessing private fields normally set by the command line
        Field resourceTemplatesField = WdtOptions.class.getDeclaredField("resourceTemplates");
        resourceTemplatesField.setAccessible(true);
        List<Path> resolveFiles =
            Arrays.asList(new File("target/test-classes/templates/resolver.yml").toPath(),
            new File("target/test-classes/templates/verrazzano.yml").toPath());
        resourceTemplatesField.set(wdtOptions, resolveFiles);

        Field imageTagField = WdtOptions.class.getDeclaredField("wdtDomainHome");
        imageTagField.setAccessible(true);
        imageTagField.set(wdtOptions, DOMAIN_HOME);

        wdtOptions.handleResourceTemplates(IMG_NAME);

        List<String> linesRead =
            Files.readAllLines(new File("target/test-classes/templates/resolver.yml").toPath());
        assertEquals(3, linesRead.size(), "Number of lines read from the file was unexpected");
        for (String line : linesRead) {
            String[] linePart = line.split(" ");
            if (linePart.length != 2) {
                fail("Resolving template file, resolver.yml, failed to resolve line: " + line);
            }
            switch (linePart[0]) {
                case "domainHome:":
                    assertEquals(DOMAIN_HOME, linePart[1], "Invalid domain home value: " + line);
                    break;
                case "image:":
                    assertEquals(IMG_NAME, linePart[1], "Invalid image name value: " + line);
                    break;
                case "domainHomeSourceType:":
                    assertEquals(DOMAIN_SOURCE, linePart[1], "Invalid domain home source type: " + line);
                    break;
                default:
                    fail("Unexpected line in resolver.yml: " + line);
            }
        }
    }
}
