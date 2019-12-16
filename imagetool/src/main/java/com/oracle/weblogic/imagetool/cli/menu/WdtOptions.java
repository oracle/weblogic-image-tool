// Copyright (c) 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
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

import com.oracle.weblogic.imagetool.api.model.InstallerType;
import com.oracle.weblogic.imagetool.api.model.WLSInstallerType;
import com.oracle.weblogic.imagetool.cachestore.CacheStore;
import com.oracle.weblogic.imagetool.cachestore.CacheStoreFactory;
import com.oracle.weblogic.imagetool.cachestore.InstallerFile;
import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.Constants;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.HttpUtil;
import com.oracle.weblogic.imagetool.wdt.DomainType;
import picocli.CommandLine.Option;

public class WdtOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WdtOptions.class);
    protected CacheStore cacheStore = new CacheStoreFactory().get();

    /**
     * Checks whether the user requested a domain to be created with WDT.
     * If so, returns the required build args to pass to docker and creates required file links to pass
     * the model, archive, variables file to build process
     *
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @return list of build args
     * @throws IOException in case of error
     */
    List<String> handleWdtArgsIfRequired(DockerfileOptions dockerfileOptions, String tmpDir,
                                         WLSInstallerType installerType) throws IOException {
        logger.entering(tmpDir);

        List<String> retVal = new LinkedList<>();
        if (wdtModelPath != null) {
            dockerfileOptions.setWdtEnabled();
            dockerfileOptions.setWdtModelOnly(wdtModelOnly);

            List<String> modelList = addWdtFilesAsList(wdtModelPath, "model", tmpDir);

            dockerfileOptions.setWdtModels(modelList);

            dockerfileOptions.setWdtDomainType(wdtDomainType);
            if (wdtDomainType != DomainType.WLS) {
                if (installerType != WLSInstallerType.FMW) {
                    throw new IOException("FMW installer is required for JRF domain");
                }
                dockerfileOptions.setRunRcu(runRcu);
            }

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
        }
        logger.exiting();
        return retVal;
    }

    /**
     * Builds a list of {@link InstallerFile} objects based on user input which are processed.
     * to download the required install artifacts
     *
     * @return list of InstallerFile
     * @throws Exception in case of error
     */
    public List<InstallerFile> gatherWdtRequiredInstallers() throws Exception {
        logger.entering();
        List<InstallerFile> result = new LinkedList<>();
        if (wdtModelPath != null) {
            logger.finer("IMG-0001", InstallerType.WDT, wdtVersion);
            InstallerFile wdtInstaller = new InstallerFile(InstallerType.WDT, wdtVersion);
            result.add(wdtInstaller);
            addWdtUrl(wdtInstaller.getKey(), cacheStore, wdtVersion);
        }
        logger.exiting(result.size());
        return result;
    }

    private void addWdtUrl(String wdtKey, CacheStore cacheStore, String wdtVersion) throws Exception {
        logger.entering(wdtKey);
        String wdtUrlKey = wdtKey + "_url";
        if (cacheStore.getValueFromCache(wdtKey) == null) {
            //if (userId == null || password == null) {
            //    throw new Exception("CachePolicy prohibits download. Add the required wdt installer to cache");
            //}
            List<String> wdtTags = HttpUtil.getWDTTags();
            String tagToMatch = "latest".equalsIgnoreCase(wdtVersion) ? wdtTags.get(0) :
                "weblogic-deploy-tooling-" + wdtVersion;
            if (wdtTags.contains(tagToMatch)) {
                String downloadLink = String.format(Constants.WDT_URL_FORMAT, tagToMatch);
                logger.info("IMG-0007", downloadLink);
                cacheStore.addToCache(wdtUrlKey, downloadLink);
            } else {
                throw new Exception("Couldn't find WDT download url for version:" + wdtVersion);
            }
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
                Files.copy(individualPath, Paths.get(tmpDir, modelFilename));
                fileList.add(modelFilename);
            } else {
                String errMsg = String.format("WDT %s file %s not found ", type, individualFile);
                throw new IOException(errMsg);
            }
        }
        return fileList;
    }


    public DomainType getWdtDomainType() {
        return wdtDomainType;
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
        description = "WDT Domain Type. Default: WLS. Supported values: ${COMPLETION-CANDIDATES}"
    )
    private DomainType wdtDomainType = DomainType.WLS;

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

}
