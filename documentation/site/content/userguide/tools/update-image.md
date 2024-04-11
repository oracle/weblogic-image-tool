---
title: "Update Image"
date: 2019-02-23
draft: false
weight: 4
description: "The update command creates a new container image by applying WebLogic patches to an existing image."
---

After you have created a container image with the Image Tool, you may want to change it from time to time.  Use the `update`
command to update the existing container images created with the Image Tool.  For example, you may want to:
* Apply a WebLogic patch
* Apply the latest PSU from Oracle
* Create a new WebLogic domain (if one did not already exist)
* Deploy a new application to an existing domain
* Modify the domain configuration (add a data source, change a port number, and such)

**NOTE**: The WebLogic Image Tool does not support a Stack Patch Bundle (SPB; see Doc ID [2764636.1](https://support.oracle.com/rs?type=doc&id=2764636.1)), because an SPB is _not_ a patch but a mechanism for applying all PSU and recommended CPU and SPU patches to a WebLogic Server installation.

The required options for the `update` command are marked.

```
Usage: imagetool update [OPTIONS]
Update WebLogic Docker image with selected patches

```
| Parameter | Definition | Default |
| --- | --- | --- |
| `--fromImage` | (Required) Container image to be extended. The provided image MUST contain an Oracle Home with middleware installed. The `fromImage` option serves as a starting point for the new image to be created. |  |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0` |  |
| `--additionalBuildCommands` | Path to a file with additional build commands. For more details, see [Additional information](#--additionalbuildcommands). | |
| `--additionalBuildFiles` | Additional files that are required by your `additionalBuildCommands`.  A comma separated list of files that should be copied to the build context. See [Additional information](#--additionalbuildfiles). |  |
| `--builder`, `-b` | Executable to process the Dockerfile. Use the full path of the executable if not on your path. | Defaults to `docker`, or, when set, to the value in environment variable `WLSIMG_BUILDER`. |
| `--buildNetwork` | Networking mode for the RUN instructions during the image build.  See `--network` for Docker `build`. | |
| `--chown` | `userid:groupid` to be used for creating files and applying middleware patches within the image. The userid and groupid must already exist in the image. | Defaults to the user and group of the Oracle Home in the provided image. |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout. | |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080` |  |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080` |  |
| `--latestPSU` | (DEPRECATED) Find and apply the latest PatchSet Update, see [Additional information](#--latestpsu). |  |
| `--opatchBugNumber` | The patch number for OPatch (patching OPatch). | `28186730` |
| `--password` | Request password for the Oracle Support `--user` on STDIN, see `--user`. |  |
| `--passwordEnv` | Environment variable containing the Oracle Support password, see `--user`. |  |
| `--passwordFile` | Path to a file containing just the Oracle Support password, see `--user`.  |  |
| `--patches` | Comma separated list of patch IDs. Example: `12345678,87654321` |  |
| `--platform` | Set the target platform to build.  Supported values: `linux/amd64` or `linux/arm64`. |   |
| `--pull` | Always attempt to pull a newer version of base images during the build. | |
| `--recommendedPatches` | (DEPRECATED) Find and apply the latest PatchSet Update and recommended patches. This takes precedence over `--latestPSU`. See [Additional information](#--recommendedpatches). |  |
| `--resourceTemplates` | One or more files containing placeholders that need to be resolved by the Image Tool. See [Resource Template Files](#resource-template-files). |  |
| `--skipcleanup` | Do not delete the build context folder, intermediate images, and failed build containers. For debugging purposes. |  |
| `--strictPatchOrdering` | Instruct OPatch to apply patches one at a time (uses `apply` instead of `napply`). |  |
| `--target` | Select the target environment in which the created image will be used. Supported values: `Default` (Docker/Kubernetes), `OpenShift`. See [Additional information](#--target). | `Default` |
| `--type` | Installer type. Supported values: `WLS`, `WLSDEV`, `WLSSLIM`, `FMW`, `IDM`, `OSB`, `OUD_WLS`, `SOA_OSB`, `SOA_OSB_B2B`, `MFT`, `WCP`, `OAM`, `OIG`, `OUD`, `OID`, `ODI`, `SOA`, `WCC`, `WCS`, `WCP` | Installer used in `fromImage` |
| `--user` | Oracle support email ID. When supplying `user`, you must supply the password either as an environment variable using `--passwordEnv`, or as a file using `--passwordFile`, or interactively, on the command line with `--password`. |  |
| `--wdtArchive` | A WDT archive ZIP file or comma-separated list of files. |  |
| `--wdtDomainHome` | Path to the `-domain_home` for WDT. | `/u01/domains/base_domain`    |
| `--wdtDomainType` | WDT domain type. Supported values: `WLS`, `JRF`, `RestrictedJRF` | `WLS` |
| `--wdtEncryptionKey` | Passphrase for WDT `-use_encryption` that will be requested on STDIN. |  |
| `--wdtEncryptionKeyEnv` | Passphrase for WDT `-use_encryption` that is provided as an environment variable. |  |
| `--wdtEncryptionKeyFile` | Passphrase for WDT `-use_encryption` that is provided as a file. |  |
| `--wdtHome` | The target folder in the image for the WDT install and models. | `/u01/wdt` |
| `--wdtJavaOptions` | Java command-line options for WDT. |  |
| `--wdtModel` | A WDT model file or a comma-separated list of files. | |
| `--wdtModelHome` | The target location in the image to copy WDT model, variable, and archive files. | `{wdtHome}/models` |
| `--wdtModelOnly` | Install WDT and copy the models to the image, but do not create the domain. | `false` |
| `--wdtOperation` | Create a new domain, or update an existing domain. Supported values: `CREATE`, `UPDATE`, `DEPLOY` | `CREATE` |
| `--wdtRunRCU` | Instruct WDT to run RCU when creating the domain. |  |
| `--wdtStrictValidation` | Use strict validation for the WDT validation method. Only applies when using model only. | `false` |
| `--wdtVariables` | A WDT variables file or comma-separated list of files. |  |
| `--wdtVersion` | WDT tool version to use. |  |

### Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections. Valid sections for update:

| Section | Available Variables | Build Stage | Timing |
| --- | --- | --- | --- |
| `before-wdt-command` | `DOMAIN_HOME` | Intermediate (WDT_BUILD) | Before WDT is installed. |
| `after-wdt-command` | `DOMAIN_HOME` | Intermediate (WDT_BUILD) | After WDT domain creation/update is complete. |
| `final-build-commands` | `JAVA_HOME` `ORACLE_HOME` `DOMAIN_HOME` | Final image | After all Image Tool actions are complete, and just before the container image is finalized. |

NOTE: Changes made in intermediate stages may not be carried forward to the final image unless copied manually.  
The Image Tool will copy the domain home and the WDT home directories to the final image.  
Changes fully contained within these directories do not need an additional `COPY` command in the `final-build-commands` section.


Each section can contain one or more valid Dockerfile commands and would look like the following:

```dockerfile
[after-fmw-install]
RUN rm /some/dir/unnecessary-file
COPY --chown=oracle:oracle files/my_additional_file.txt /u01

[final-build-commands]
LABEL owner="middleware team"
```

#### `--additionalBuildFiles`

This option provides a way to supply additional files to the image build command.
All provided files and directories are copied directly under the `files` subfolder of the build context.  
To get those files into the image, additional build commands must be provided using the `additionalBuildCommands` options.
Access to these files using a build command, such as `COPY` or `ADD`, should use the original filename
with the folder prefix, `files/`.  For example, if the
original file was provided as `--additionalBuildFiles /scratch/test1/convenience.sh`, the Docker build command `COPY`
provided in `--additionalBuildCommands` should look like
`COPY --chown=oracle:oracle files/convenience.sh /my/internal/image/location`.  
Because Image Tool uses multi-stage
builds, it is important to place the build command (like `COPY`) in the appropriate section of the `Dockerfile` based
on when the build needs access to the file.  For example, if the file is needed in the final image and not for
installation or domain creation steps, use the `final-build-commands` section so that the `COPY` command occurs in the
final stage of the image build.  Or, if the file needs to change the Oracle Home prior to domain creation, use
the `after-fmw-install` or `before-wdt-command` sections.

#### `--latestPSU`

The `latestPSU` option will continue to be supported for the CREATE and REBASE option, but has been deprecated for use in the
UPDATE option.  Because of the number of patches and their size, using `latestPSU` as an update to an existing image can
increase the size of the image _significantly_, and is not recommended.

#### `--recommendedPatches`

The `recommendedPatches` option will continue to be supported for the CREATE and REBASE option, but has been deprecated for use in the
UPDATE option.  Because of the number of patches and their size, using `recommendedPatches` as an update to an existing image can
increase the size of the image _significantly_, and is not recommended.

#### `--target`

By default, the generated WLS domain in your image will use the best practices defined by Oracle WebLogic Server.  
The `target` option allows you to toggle the defaults so that the generated domain is easier to use in the target
environment.  For example, the `--target OpenShift` option will change the file permissions in the domain directory
so that the group permissions match the user permissions.

| Target | Default File Permissions | Default File Ownership |
| --- | --- | --- |
| `Default` | `rwxr-x---` | `oracle:oracle` |
| `OpenShift` | `rwxrwx---` | `oracle:root` |

#### Resource Template Files

If provided, the file or files provided with `--resourceTemplates` will be overwritten. For known tokens,
the placeholders will be replaced with values according to the following table.  

**Note:** Placeholders must follow the Mustache syntax, like `{{imageName}}` or `{{{imageName}}}`.

| Token Name | Value Description |
| --- | --- |
| `domainHome` | The value provided to the Image Tool with `--wdtDomainHome`. |
| `domainHomeSourceType` | `PersistentVolume` (default), `FromModel` if `--wdtModelOnly`, or `Image` if the domain is created in the image with WDT. |
| `imageName` | The value provided to the Image Tool with `--tag`. |
| `modelHome` | The value provided to the Image Tool with `--wdtModelHome`. |

#### Use an argument file

You can save all arguments passed for the Image Tool in a file, then use the file as a parameter.

For example, create a file called `build_args`:

```bash
update
--fromImage weblogic:12.2.1.3.0
--tag wls:122130-patched
--patches 123456
--user acmeuser@mycompany.com
--passwordEnv MYPWD

```

Use it on the command line, as follows:

```bash
$ imagetool @/path/to/build_args
```


### Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

- Update an image named `sample:1.0` by applying the latest PSU and tag it as `sample:1.1`.
    ```bash
    $ imagetool update --fromImage sample:1.0 --tag sample:1.1 --latestPSU --user test@xyz.com --passwordEnv MYVAR
    ```

- Update an image named `sample:1.0` with the selected patches applied and tag it as `sample:1.1`.
    ```bash
    $ imagetool update --fromImage sample:1.0 --tag sample:1.1 --user test@xyz.com --password hello --patches 12345678,87654321
    ```

- Update an image named `wls:12.2.1.3.0` by creating a new WebLogic domain using WDT and tag it as `mydomain:1`.  The WDT
installer is accessed from the cache with key wdt_1.1.1.  The model and archive to use are located in a subfolder named `wdt`.
    ```bash
    $ imagetool update --fromImage wls:12.2.1.3.0 --tag mydomain:1 --wdtArchive ./wdt/my_domain.zip --wdtModel ./wdt/my_domain.yaml --wdtVersion 1.1.1
    ```

- Use `deployApps` from WDT to deploy a new application, the WLS Metrics Exporter.  The model and archive to use are
located in a subfolder named `wdt`.
    ```bash
    $ imagetool update --tag mydomain:2 --fromImage mydomain:1 --wdtOperation deploy --wdtArchive ./wdt/exporter_archive.zip --wdtModel ./wdt/exporter_model.yaml --wdtVersion 1.1.1   
    ```
