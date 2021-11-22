// Copyright (c) 2019, 2021, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.weblogic.imagetool.cli.menu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.oracle.weblogic.imagetool.logging.LoggingFacade;
import com.oracle.weblogic.imagetool.logging.LoggingFactory;
import com.oracle.weblogic.imagetool.util.DockerfileOptions;
import com.oracle.weblogic.imagetool.util.DomainHomeSourceType;
import com.oracle.weblogic.imagetool.util.ResourceTemplateOptions;
import com.oracle.weblogic.imagetool.util.Utils;
import picocli.CommandLine.Option;

public class WdtFullOptions extends WdtBaseOptions {

    private static final LoggingFacade logger = LoggingFactory.getLogger(WdtFullOptions.class);

    /**
     * Return true if the user specified a WDT model or WDT archive on the command line.
     * @return true if WDT was selected by the user
     */
    public boolean isUsingWdt() {
        return wdtModelOnly || isWdtModelProvided();
    }

    /**
     * Return true if the user specified the wdtModelOnly option, and
     * WDT should not run a create or update script.
     * @return true if the WDT script should not run during the build.
     */
    public boolean modelOnly() {
        return wdtModelOnly;
    }

    /**
     * Checks whether the user requested a domain to be created with WDT.
     * If so,  creates required file links to pass the model, archive, variables file to build process.
     *
     * @param tmpDir the tmp directory which is passed to docker as the build context directory
     * @throws IOException in case of error
     */
    @Override
    public void handleWdtArgs(DockerfileOptions dockerfileOptions, String tmpDir) throws IOException {
        logger.entering(tmpDir);
        if (!isUsingWdt()) {
            logger.exiting();
            return;
        }
        super.handleWdtArgs(dockerfileOptions, tmpDir);

        String encryptionKey = Utils.getPasswordFromInputs(encryptionKeyStr, encryptionKeyFile, encryptionKeyEnv);
        if (encryptionKey != null) {
            dockerfileOptions.setWdtEncryptionKey(encryptionKey);
        }

        dockerfileOptions.setWdtEnabled()
            .setDomainHome(wdtDomainHome)
            .setJavaOptions(wdtJavaOptions)
            .setWdtDomainType(wdtDomainType)
            .setRunRcu(runRcu)
            .setWdtStrictValidation(wdtStrictValidation)
            .setWdtModelOnly(wdtModelOnly);

        logger.exiting();
    }

    /**
     * Resolve variables in the provided list of resource template files.
     * See --resourceTemplates.  For example, WDT -target vz is used to generate a custom resource.
     * In the generated file(s), WDT will not know what the image name is and will leave a placeholder.
     * This function provides the values for variables that are known during image build.
     */
    public void handleResourceTemplates(String imageName) throws IOException {
        DomainHomeSourceType domainType = DomainHomeSourceType.PV;
        if (modelOnly()) {
            domainType = DomainHomeSourceType.MODEL;
        } else if (isUsingWdt()) {
            domainType = DomainHomeSourceType.IMAGE;
        }

        ResourceTemplateOptions options = new ResourceTemplateOptions()
            .domainHome(wdtDomainHome)
            .imageName(imageName)
            .modelHome(wdtModelHome())
            .domainHomeSourceType(domainType);

        // resolve parameters in the list of mustache templates returned by gatherFiles()
        Utils.writeResolvedFiles(resourceTemplates, options);
    }

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

    @Option(
        names = {"--resourceTemplates"},
        split = ",",
        description = "Resolve variables in the resource template(s) with information from the image tool build."
    )
    List<Path> resourceTemplates;
}
