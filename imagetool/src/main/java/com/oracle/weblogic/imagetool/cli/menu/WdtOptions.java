// Copyright (c) 2019, 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.oracle.weblogic.imagetool.api.model.CachedFile;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import com.oracle.weblogic.imagetool.installer.InstallerType;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

public class WdtOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WdtOptions.class);
    protected CacheStore cacheStore = new CacheStoreFactory().get();

    /**
     * Checks whether the user requested a domain to be created with WDT.
     * If so,  creates required file links to pass the model, archive, variables file to build process.
     *
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @throws IOException in case of error
     */
    List<String> handleWdtArgs(DockerfileOptions dockerfileOptions, String tmpDir) throws IOException {
        List<String> result = new LinkedList<>();
        if (wdtModelPath == null) {
            return result;
        }

        logger.entering(tmpDir);
        encryptionKey = Utils.getPasswordFromInputs(encryptionKeyStr, encryptionKeyFile, encryptionKeyEnv);
        if (encryptionKey != null) {
            dockerfileOptions.setWdtUseEncryption(true);
            result.add(Constants.BUILD_ARG);
            result.add("WDT_ENCRYPTION_KEY=" + encryptionKey);
        }

        dockerfileOptions.setWdtEnabled();
        dockerfileOptions.setWdtModelOnly(wdtModelOnly);

        List<String> modelList = addWdtFilesAsList(wdtModelPath, "model", tmpDir);

        dockerfileOptions.setWdtModels(modelList);

        dockerfileOptions.setWdtDomainType(wdtDomainType);
        dockerfileOptions.setRunRcu(runRcu);

        if (wdtArchivePath != null) {

            List<String> archiveList = addWdtFilesAsList(wdtArchivePath, "archive", tmpDir);

            dockerfileOptions.setWdtArchives(archiveList);
        }
        dockerfileOptions.setDomainHome(wdtDomainHome);

        dockerfileOptions.setJavaOptions(wdtJavaOptions);

        if (wdtVariablesPath != null && Files.isRegularFile(wdtVariablesPath)) {
            String wdtVariableFilename = wdtVariablesPath.getFileName().toString();
            Files.copy(wdtVariablesPath, Paths.get(tmpDir, wdtVariableFilename));
            //Until WDT supports multiple variable files, take single file argument from CLI and convert to list
            dockerfileOptions.setWdtVariables(Collections.singletonList(wdtVariableFilename));
        }

        dockerfileOptions.setWdtStrictValidation(wdtStrictValidation);

        CachedFile wdtInstaller = new CachedFile(InstallerType.WDT, wdtVersion);
        Path wdtfile = wdtInstaller.copyFile(cacheStore, tmpDir);
        dockerfileOptions.setWdtInstallerFilename(wdtfile.getFileName().toString());

        logger.exiting();
        return result;
    }

    private List<String> addWdtFilesAsList(Path fileArg, String type, String tmpDir) throws IOException {
        String[] listOfFiles = fileArg.toString().split(",");
        List<String> fileList = new ArrayList<>();

        for (String individualFile : listOfFiles) {
            Path individualPath = Paths.get(individualFile);
            if (Files.isRegularFile(individualPath)) {
                String modelFilename = individualPath.getFileName().toString();
                Files.copy(individualPath, Paths.get(tmpDir, modelFilename));
                fileList.add(modelFilename);
            } else {
                String errMsg = String.format("WDT %s file %s not found ", type, individualFile);
                throw new IOException(errMsg);
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
        names = {"--wdtDomainType"},
        description = "WDT Domain Type (-domain_type). Default: WLS. Supported values: WLS, JRF, or RestrictedJRF"
    )
    @SuppressWarnings("FieldCanBeLocal")
    private String wdtDomainType = "WLS";

    @Option(
        names = "--wdtRunRCU",
        description = "instruct WDT to run RCU when creating the Domain"
    )
    @SuppressWarnings("FieldCanBeLocal")
    private boolean runRcu = false;

    @Option(
        names = {"--wdtDomainHome"},
        description = "pass to the -domain_home for wdt",
        defaultValue = "/u01/domains/base_domain"
    )
    private String  wdtDomainHome;

    @Option(
        names = {"--wdtJavaOptions"},
        description = "Java command line options for WDT"
    )
    private String wdtJavaOptions;

    @Option(
        names = {"--wdtModelOnly"},
        description = "Install WDT and copy the models to the image, but do not create the domain. "
            + "Default: ${DEFAULT-VALUE}."
    )
    @SuppressWarnings("FieldCanBeLocal")
    private boolean wdtModelOnly = false;

    @Option(
        names = {"--wdtStrictValidation"},
        description = "Use strict validation for the WDT validation method. Only applies when using model only.  "
            + "Default: ${DEFAULT-VALUE}."
    )
    @SuppressWarnings("FieldCanBeLocal")
    private boolean wdtStrictValidation = false;

    private String encryptionKey;

    @Option(
        names = {"--wdtEncryptionKey"},
        interactive = true,
        arity = "0..1",
        paramLabel = "<passphrase>",
        description = "Enter the passphrase to decrypt the WDT model"
    )
    private String encryptionKeyStr;

    @Option(
        names = {"--wdtEncryptionKeyEnv"},
        paramLabel = "<environment variable name>",
        description = "environment variable containing the passphrase to decrypt the WDT model"
    )
    private String encryptionKeyEnv;

    @Option(
        names = {"--wdtEncryptionKeyFile"},
        paramLabel = "<passphrase file>",
        description = "path to file the passphrase to decrypt the WDT model"
    )
    private Path encryptionKeyFile;
}
