/* Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. */

package com.oracle.weblogicx.imagebuilder.cli.menu;

import com.oracle.weblogicx.imagebuilder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.impl.InstallerFile;
import com.oracle.weblogicx.imagebuilder.impl.PatchFile;
import com.oracle.weblogicx.imagebuilder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.util.Constants;
import com.oracle.weblogicx.imagebuilder.util.Utils;
import com.oracle.weblogicx.imagebuilder.util.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@Command(
        name = "update",
        description = "Update WebLogic docker image with selected patches",
        version = "1.0",
        requiredOptionMarker = '*',
        abbreviateSynopsis = true
)
public class UpdateImage extends ImageOperation {

    private Logger logger = Logger.getLogger("com.oracle.weblogix.imagebuilder.builder");

    @Override
    public CommandResponse call() throws Exception {
        Instant startTime = Instant.now();

        FileHandler fileHandler = setupLogger(unmatchedOptions.contains(Constants.CLI_OPTION));
        Path tmpDir = null;
        Path tmpDir2 = null;

        try {

            handleProxyUrls();

            // check credentials if useCache option allows us to download artifacts
            if (!ARUUtil.checkCredentials(userId, password)) {
                return new CommandResponse(-1, "User credentials do not match");
            }

            List<String> cmdBuilder = getInitialBuildCmd();

            if (fromImage == null || fromImage.isEmpty()) {
                return new CommandResponse(-1, "update requires a base image. use --fromImage to specify base image");
            } else {
                cmdBuilder.add(Constants.BUILD_ARG);
                cmdBuilder.add("BASE_IMAGE=" + fromImage);
            }

            tmpDir2 = Files.createTempDirectory(Paths.get(System.getProperty("user.home")), null);
            System.out.println("tmpDir2:" + tmpDir2);
            Utils.copyResourceAsFile("/test-update-env.sh",
                    tmpDir2.toAbsolutePath().toString() + File.separator + "test-env.sh", true);

            List<String> imageEnvCmd = Utils.getDockerRunCmd(tmpDir2, fromImage, "test-env.sh");

            System.out.println("cmdToExec:" + String.join(" ", imageEnvCmd));
            Properties baseImageProperties = Utils.runDockerCommand(imageEnvCmd);

            String oracleHome = baseImageProperties.getProperty("ORACLE_HOME", null);
            if (oracleHome == null) {
                return new CommandResponse(-1, "ORACLE_HOME env variable undefined in base image: " + fromImage);
            }
            installerType = WLSInstallerType.fromValue(baseImageProperties.getProperty("WLS_TYPE", WLSInstallerType.WLS.toString()));
            installerVersion = baseImageProperties.getProperty("WLS_VERSION", Constants.DEFAULT_WLS_VERSION);

            String opatchVersion = baseImageProperties.getProperty("OPATCH_VERSION");
            opatch_1394_required = (Utils.compareVersions(installerVersion, Constants.DEFAULT_WLS_VERSION) >=0 &&
                    Utils.compareVersions(opatchVersion, "13.9.4.0.0") < 0);

            String pkgMgr = Utils.getPackageMgrStr(baseImageProperties.getProperty("ID", "ol"));
            if (!Utils.isEmptyString(pkgMgr)) {
                filterStartTags.add(pkgMgr);
            }

            baseImageProperties.keySet().forEach( x -> System.out.println(x + "=" + baseImageProperties.getProperty((String) x)));

            if (latestPSU || !patches.isEmpty()) {
                String lsInvFile = tmpDir2.toAbsolutePath().toString() + File.separator + "opatch-lsinventory.txt";
                if (Files.exists(Paths.get(lsInvFile)) && Files.size(Paths.get(lsInvFile)) > 0) {
                    System.out.println("**** opatch-lsinventory file exists ****");
                    Set<String> toValidateSet = new HashSet<>();
                    if (latestPSU) {
                        toValidateSet.add(ARUUtil.getLatestPSUNumber(installerType.toString(), installerVersion, userId, password));
                    }
                    toValidateSet.addAll(patches);
                    ValidationResult validationResult = ARUUtil.validatePatches(lsInvFile, new ArrayList<>(toValidateSet),
                            installerType.toString(), installerVersion, userId, password);
                    if (!validationResult.isSuccess()) {
                        return new CommandResponse(-1, validationResult.getErrorMessage());
                    } else {
                        System.out.println("patch conflict check successful");
                    }
                } else {
                    return new CommandResponse(-1, "inventory file missing. required to check for conflicts");
                }
            }

            // create a tmp directory for user.
            tmpDir = Files.createTempDirectory(null);
            String tmpDirPath = tmpDir.toAbsolutePath().toString();
            //System.out.println("tmpDir = " + tmpDirPath);
            Path tmpPatchesDir = Files.createDirectory(Paths.get(tmpDirPath, "patches"));
            Files.createFile(Paths.get(tmpPatchesDir.toAbsolutePath().toString(), "dummy.txt"));

            // this handles opatch_1394 install file.
            //cmdBuilder.addAll(handleInstallerFiles(tmpDir));

            // resolve required patches
            cmdBuilder.addAll(handlePatchFiles(tmpDir, tmpPatchesDir));

            //Utils.copyResourceAsFile("/Dockerfile.update", tmpDirPath + File.separator + "Dockerfile");
            // create dockerfile
            Utils.replacePlaceHolders(tmpDirPath + File.separator + "Dockerfile", "/Dockerfile.update", filterStartTags, "/Dockerfile.ph");

            // add directory to pass the context
            cmdBuilder.add(tmpDirPath);

            logger.info("docker cmd = " + String.join(" ", cmdBuilder));
            Utils.runDockerCommand(cmdBuilder, dockerLog);

        } catch (Exception ex) {
            return new CommandResponse(-1, ex.getMessage());
        } finally {
            Utils.deleteFilesRecursively(tmpDir);
            Utils.deleteFilesRecursively(tmpDir2);
            if (fileHandler != null) {
                fileHandler.close();
                logger.removeHandler(fileHandler);
            }
        }
        Instant endTime = Instant.now();
        return new CommandResponse(0, "build successful in " + Duration.between(startTime, endTime).getSeconds()  + "s. image tag: " + imageTag);
    }

    @Override
    List<String> handlePatchFiles(Path tmpDir, Path tmpPatchesDir) throws Exception {
        if (opatch_1394_required) {
            addOPatch1394ToImage(tmpDir);
        }
        return super.handlePatchFiles(tmpDir, tmpPatchesDir);
    }

    //    /**
//     * Builds a list of {@link InstallerFile} objects based on user input which are processed
//     * to download the required install artifacts
//     * @return list of InstallerFile
//     * @throws Exception in case of error
//     */
//    protected List<InstallerFile> gatherRequiredInstallers() throws Exception {
//        List<InstallerFile> retVal = new LinkedList<>();
//        if (opatch_1394_required) {
//            retVal.add(new InstallerFile(Constants.OPATCH_1394_KEY, true, userId, password));
//            filterStartTags.add("OPATCH_1394");
//        }
//        return retVal;
//    }

    @Option(
            names = { "--fromImage" },
            description = "Docker image to use as base image.",
            required = true
    )
    private String fromImage;

    private boolean opatch_1394_required = false;
}
