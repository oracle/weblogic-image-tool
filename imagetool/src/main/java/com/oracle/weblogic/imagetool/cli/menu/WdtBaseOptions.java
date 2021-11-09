// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import picocli.CommandLine.Option;

import static com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory.cache;

public class WdtBaseOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WdtBaseOptions.class);

    /**
     * Return true if the user provided a WDT model or WDT archive on the command line.
     * @return true if the user provided a WDT file as input on the command line.
     */
    public boolean isWdtModelProvided() {
        return wdtModelPath != null || wdtArchivePath != null || wdtVariablesPath != null;
    }

    /**
     * Return the value provided for --wdtModelHome.
     * This value is used as the location to install the provided WDT models, archives, and variable files.
     * @return value of --wdtModelHome
     */
    public String wdtModelHome() {
        return wdtModelHome;
    }

    /**
     * Add the provided WDT files and WDT installer to the Docker build context folder.
     *
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @throws IOException in case of error
     */
    void handleWdtArgs(DockerfileOptions dockerfileOptions, String tmpDir) throws IOException {
        logger.entering(tmpDir);

        dockerfileOptions.setWdtEnabled()
            .setWdtModelHome(wdtModelHome);

        if (wdtHome != null) {
            dockerfileOptions.setWdtHome(wdtHome);
        }

        if (wdtModelPath != null) {
            List<String> modelList = addWdtFilesAsList(wdtModelPath, "model", tmpDir);
            dockerfileOptions.setWdtModels(modelList);
        }

        if (wdtArchivePath != null) {
            List<String> archiveList = addWdtFilesAsList(wdtArchivePath, "archive", tmpDir);
            dockerfileOptions.setWdtArchives(archiveList);
        }

        if (wdtVariablesPath != null) {
            List<String> variablesList = addWdtFilesAsList(wdtVariablesPath, "variables", tmpDir);
            dockerfileOptions.setWdtVariables(variablesList);
        }

        CachedFile wdtInstaller = new CachedFile(InstallerType.WDT, wdtVersion);
        Path wdtfile = wdtInstaller.copyFile(cache(), tmpDir);
        dockerfileOptions.setWdtInstallerFilename(wdtfile.getFileName().toString());

        logger.exiting();
    }

    private List<String> addWdtFilesAsList(Path fileArg, String type, String tmpDir) throws IOException {
        String[] listOfFiles = fileArg.toString().split(",");
        List<String> fileList = new ArrayList<>();

        for (String individualFile : listOfFiles) {
            Path individualPath = Paths.get(individualFile);
            if (Files.isRegularFile(individualPath)) {
                String modelFilename = individualPath.getFileName().toString();
                logger.info("IMG-0043", individualPath);
                Files.copy(individualPath, Paths.get(tmpDir, modelFilename));
                fileList.add(modelFilename);
            } else {
                String errMsg = String.format("WDT %s file %s not found ", type, individualFile);
                throw new FileNotFoundException(errMsg);
            }
        }
        return fileList;
    }

    @Option(
        names = {"--wdtModel"},
        description = "path to the WDT model file that defines the Domain to create"
    )
    private Path wdtModelPath;

    @Option(
        names = {"--wdtArchive"},
        description = "path to the WDT archive file used by the WDT model"
    )
    private Path wdtArchivePath;

    @Option(
        names = {"--wdtVariables"},
        description = "path to the WDT variables file for use with the WDT model"
    )
    private Path wdtVariablesPath;

    @Option(
        names = {"--wdtVersion"},
        description = "WDT tool version to use",
        defaultValue = "latest"
    )
    private String wdtVersion;

    @Option(
        names = {"--wdtModelHome"},
        description = "Copy the models to the location in the image Default: WDT_HOME/models"
    )
    private String wdtModelHome;

    @Option(
        names = {"--wdtHome"},
        description = "Set the base directory in the target image for WDT models and the WDT installer."
    )
    private String wdtHome;
}
