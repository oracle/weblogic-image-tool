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
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;


public class WdtBaseOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WdtBaseOptions.class);
    public static final String WDT_HOME_LABEL = "<WDT home directory>";

    /**
     * Return true if the user provided WDT models, WDT archives, or WDT variables on the command line.
     * @return true if the user provided at least one WDT file as input on the command line.
     */
    public boolean userProvidedFiles() {
        return wdtModelPath != null || wdtArchivePath != null || wdtVariablesPath != null;
    }

    /**
     * Return true if the user did not specify --wdtVersion=NONE.
     * @return true if the tool should install WDT binaries.
     */
    public boolean skipWdtInstaller() {
        return wdtVersion.equalsIgnoreCase("NONE");
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
    public void handleWdtArgs(DockerfileOptions dockerfileOptions, String tmpDir) throws IOException {
        logger.entering(tmpDir);

        if (!userProvidedFiles() && skipWdtInstaller()) {
            // if user did not provide models, variables, archives, or a WDT installer, there is nothing to do.
            throw new IllegalArgumentException(Utils.getMessage("IMG-0104"));
        }
        dockerfileOptions.setWdtEnabled();

        dockerfileOptions.setWdtHome(wdtHome).setWdtModelHome(wdtModelHome);

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

        if (!skipWdtInstaller()) {
            CachedFile wdtInstaller = new CachedFile(InstallerType.WDT, wdtVersion);
            Path wdtfile = wdtInstaller.copyFile(tmpDir);
            dockerfileOptions.setWdtInstallerFilename(wdtfile.getFileName().toString());
        }
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
                throw new FileNotFoundException(Utils.getMessage("IMG-0102",type, individualFile));
            }
        }
        return fileList;
    }

    @Option(
        names = {"--wdtModel"},
        description = "A WDT model file (or a comma-separated list of files)."
    )
    private Path wdtModelPath;

    @Option(
        names = {"--wdtArchive"},
        description = "A WDT archive zip file, if needed (or comma-separated list of files)."
    )
    private Path wdtArchivePath;

    @Option(
        names = {"--wdtVariables"},
        description = "A WDT variables file, if needed (or comma-separated list of files)."
    )
    private Path wdtVariablesPath;

    @Option(
        names = {"--wdtVersion"},
        description = "WDT version to use.  Default: ${DEFAULT-VALUE}.",
        defaultValue = "latest"
    )
    private String wdtVersion;

    @Option(
        names = {"--wdtModelHome"},
        description =
            "The target location in the image to copy WDT model, variable, and archive files.  Default: WDT_HOME/models"
    )
    private String wdtModelHome;

    @Option(
        names = {"--wdtHome"},
        paramLabel = WDT_HOME_LABEL,
        description = "The target folder in the image for the WDT install and models. Default: ${DEFAULT-VALUE}.",
        defaultValue = "/u01/wdt"
    )
    private String wdtHome;
}
