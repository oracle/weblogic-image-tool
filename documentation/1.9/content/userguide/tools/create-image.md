---
title: "Create Image"
date: 2019-02-23
draft: false
weight: 1
description: "The create command creates a new Docker image and installs the requested Java and WebLogic software."
---


The `create` command helps build a WebLogic Docker image from a given base OS image. The required option for the command is marked. There are a number of optional parameters for the create feature.

```
Usage: imagetool create [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
| `--additionalBuildCommands` | Path to a file with additional build commands. For more details, see [Additional information](#additional-information). |
| `--additionalBuildFiles` | Additional files that are required by your `additionalBuildCommands`.  A comma separated list of files that should be copied to the build context. |
| `--builder`, `-b` | Executable to process the Dockerfile. Use the full path of the executable if not on your path. | `docker`  |
| `--buildNetwork` | Networking mode for the RUN instructions during the image build.  See `--network` for Docker `build`.  |   |
| `--chown` | `userid:groupid` for JDK/Middleware installs and patches.  | `oracle:oracle` |
| `--docker` | (DEPRECATED) Path to the Docker executable. Use `--builder` instead.  |  `docker` |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout.  |  |
| `--fromImage` | Docker image to use as a base image when creating a new image. | `ghcr.io/oracle/oraclelinux:7-slim`  |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080`  |   |
| `--installerResponseFile` | One or more custom response files. A comma separated list of paths to installer response files. Overrides the default responses for the Oracle silent installer.  |   |
| `--inventoryPointerFile` | Path to custom inventory pointer file.  |   |
| `--inventoryPointerInstallLoc` | Target location for the inventory pointer file.  |   |
| `--jdkVersion` | Version of the server JDK to install.  | `8u202`  |
| `--latestPSU` | Find and apply the latest PatchSet Update.  |   |
| `--recommendedPatches` | Find and apply the latest PatchSet Update and recommended patches. This takes precedence over --latestPSU  |   |
| `--opatchBugNumber` | The patch number for OPatch (patching OPatch).  | `28186730`  |
| `--packageManager` | Override the default package manager for the base image's operating system. Supported values: `APK`, `APTGET`, `NONE`, `OS_DEFAULT`, `YUM`, `ZYPPER`  | `OS_DEFAULT`  |
| `--password` | Request password for the Oracle Support `--user` on STDIN, see `--user`.  |   |
| `--passwordEnv` | Environment variable containing the Oracle Support password, see `--user`.  |   |
| `--passwordFile` | Path to a file containing just the Oracle Support password, see `--user`.  |   |
| `--patches` | Comma separated list of patch IDs. Example: `12345678,87654321`  |   |
| `--pull` | Always attempt to pull a newer version of base images during the build.  |   |
| `--resourceTemplates` | One or more files containing placeholders that need to be resolved by the Image Tool. See [Resource Template Files](#resource-template-files). |   |
| `--strictPatchOrdering` |  Instruct OPatch to apply patches one at a time (uses `apply` instead of `napply`). |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0`  |   |
| `--type` | Installer type. Supported values: `WLS`, `WLSDEV`, `WLSSLIM`, `FMW`, `IDM`, `OSB`, `OUD_WLS`, `SOA_OSB`, `SOA_OSB_B2B`, `MFT`, `WCP`, `OAM`, `OIG`, `OUD`, `OID`, `SOA`, `WCC`, `WCS`, `WCP`  | `WLS`  |
| `--user` | Oracle support email ID.  |   |
| `--version` | Installer version. | `12.2.1.3.0`  |
| `--wdtArchive` | Path to the WDT archive file used by the WDT model.  |   |
| `--wdtDomainHome` | Path to the `-domain_home` for WDT.  | `/u01/domains/base_domain`  |
| `--wdtDomainType` | WDT domain type. Supported values: `WLS`, `JRF`, `RestrictedJRF`  | `WLS`  |
| `--wdtEncryptionKey` | Passphrase for WDT `-use_encryption` that will be requested on STDIN. |   |
| `--wdtEncryptionKeyEnv` | Passphrase for WDT `-use_encryption` that is provided as an environment variable. |   |
| `--wdtEncryptionKeyFile` | Passphrase for WDT `-use_encryption` that is provided as a file. |   |
| `--wdtJavaOptions` | Java command-line options for WDT.  |   |
| `--wdtModel` | Path to the WDT model file that defines the domain to create.  |   |
| `--wdtModelOnly` | Install WDT and copy the models to the image, but do not create the domain.  | `false`  |
| `--wdtRunRCU` | Instruct WDT to run RCU when creating the domain.  |   |
| `--wdtStrictValidation` | Use strict validation for the WDT validation method. Only applies when using model only.  | `false`  |
| `--wdtVariables` | Path to the WDT variables file for use with the WDT model.  |   |
| `--wdtVersion` | WDT version to use.  | `latest`  |

### Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections.
Valid sections for create are:

| Section | Build Stage | Timing |
| --- | --- | --- |
| `package-manager-packages` | All | A list of OS packages, such as `ftp gzip`, separated by line or space. |
| `before-jdk-install` | Intermediate (JDK_BUILD) | Before the JDK is installed. |
| `after-jdk-install` | Intermediate (JDK_BUILD) | After the JDK is installed. |
| `before-fmw-install` | Intermediate (WLS_BUILD) | Before the Oracle Home is created. |
| `after-fmw-install` | Intermediate (WLS_BUILD) | After all of the Oracle middleware installers are finished. |
| `before-wdt-command` | Intermediate (WDT_BUILD) | Before WDT is installed. |
| `after-wdt-command` | Intermediate (WDT_BUILD) | After WDT domain creation/update is complete. |
| `final-build-commands` | Final image | After all Image Tool actions are complete, and just before the Docker image is finalized. |

**NOTE**: Changes made in intermediate stages may not be carried forward to the final image unless copied manually.  
The Image Tool will copy the Java Home, Oracle Home, domain home, and WDT home directories to the final image.  
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
create
--type wls
--version 12.2.1.3.0
--tag wls:122130
--user acmeuser@mycompany.com
--httpProxyUrl http://mycompany-proxy:80
--httpsProxyUrl http://mycompany-proxy:80
--passwordEnv MYPWD

```

Use it on the command line, as follows:

```bash
$ imagetool @/path/to/build_args
```


### Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

The commands below assume that all the required JDK, WLS, or FMW (WebLogic infrastructure) installers have been downloaded
 to the cache directory. Use the [cache]({{< relref "/userguide/tools/cache.md" >}}) command to set it up.

- Create an image named `sample:wls` with the WebLogic installer 12.2.1.3.0, server JDK 8u202, and latest PSU applied.
    ```bash
    $ imagetool create --tag sample:wls --latestPSU --user testuser@xyz.com --password hello
    ```

- Create an image named `sample:wdt` with the same options as above and create a domain with [WebLogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/).
    ```bash
    $ imagetool create --tag sample:wdt --latestPSU --user testuser@xyz.com --password hello --wdtModel /path/to/model.json --wdtVariables /path/to/variables.json --wdtVersion 0.16
    ```
    If `wdtVersion` is not provided, the tool uses the latest release.

- Create an image named `sample:patch` with the selected patches applied.
    ```bash
    $ imagetool create --tag sample:patch --user testuser@xyz.com --password hello --patches 12345678,p87654321
    ```
    The patch numbers may or may not start with '`p`'.
