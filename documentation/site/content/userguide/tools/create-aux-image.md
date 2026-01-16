---
title: "Create Auxiliary Image"
date: 2019-02-23
draft: false
weight: 2
description: "The createAuxImage command creates a new container image with WDT install and optional models, archives, and variables files."
---


The `createAuxImage` command helps build a container image from a given base OS image.
Auxiliary images are very small images providing the WDT install files with WDT models, archives, and variables
for [WebLogic Kubernetes Operator - Auxiliary Images](https://oracle.github.io/weblogic-kubernetes-operator/managing-domains/model-in-image/auxiliary-images/).
These images are an alternative approach for including Model-in-Image model files, application archive files, WebLogic Deploying Tooling installation files, or other types of files,
in your WebLogic Server Kubernetes Operator environment.

There are a number of optional parameters for this feature. The required option for the command is marked in the following table.

```
Usage: imagetool createAuxImage [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
| `--tag` | **(Required)** Tag for the final build image. Example: `store/oracle/mydomain:1`  |   |
| `--additionalBuildCommands` | Path to a file with additional build commands. For more details, see [Additional information](#--additionalbuildcommands). |
| `--additionalBuildFiles` | Additional files that are required by your `additionalBuildCommands`.  A comma separated list of files that should be copied to the build context. See [Additional information](#--additionalbuildfiles). |
| `--builder`, `-b` | Executable to process the Dockerfile. Use the full path of the executable if not on your path. | Defaults to `docker`, or, when set, to the value in environment variable `WLSIMG_BUILDER`. |
| `--buildNetwork` | Networking mode for the RUN instructions during the image build.  See `--network` for Docker `build`.  |   |
| `--chown` | `userid:groupid` to be used for creating files within the image, such as the WDT installer, WDT model, and WDT archive. If the user or group does not exist in the image, they will be added with useradd/groupadd. | `oracle:oracle` |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout.  |  |
| `--fromImage` | Container image to use as a base image when creating a new image. | `busybox`  |
| `--fromImageProperties` | Properties that describe the `--fromImage`. If not provided, docker run will be used to inspect the `--fromImage` image. See [Custom Base Images](#custom-base-images) |  |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080`  |   |
| `--load` | Load into local repository - only valid for docker building multi platforms images, user is assumed logged in to the repo registry already.  All others are ignored.|
| `--packageManager` | Override the default package manager for the base image's operating system. Supported values: `APK`, `APTGET`, `NONE`, `YUM`, `ZYPPER`  |   |
| `--platform` | Target platform for the image: linux/amd64 or linux/arm64|
| `--pull` | Always attempt to pull a newer version of base images during the build.  |   |
| `--push` | Push to remote repository - only valid for docker building multi platforms images, user is assumed logged in to the repo registry already.  All others are ignored. |
| `--skipcleanup` | Do not delete the build context folder, intermediate images, and failed build containers. For debugging purposes.  |   |
| `--target` | Select the target environment in which the created image will be used. Supported values: `Default` (Docker/Kubernetes), `OpenShift`. See [Additional information](#--target). | `Default`  |
| `--wdtArchive` | A WDT archive ZIP file or comma-separated list of files.  |   |
| `--wdtHome` | The target folder in the image for the WDT install and models.  | `/auxiliary`  |
| `--wdtModel` | A WDT model file or a comma-separated list of files.  |   |
| `--wdtModelHome` | The target location in the image to copy WDT model, variable, and archive files. | `{wdtHome}/models` |
| `--wdtVariables` | A WDT variables file or comma-separated list of files.  |   |
| `--wdtVersion` | WDT version to be installed in the container image in `{wdtHome}/weblogic-deploy`. For more details, see [Additional information](#--wdtversion). | `latest`  |

### Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections.
Valid sections for `createAuxImage` are:

| Section | Available Variables | Build Stage | Timing |
| --- | --- | --- | --- |
| `initial-build-commands` | None | All | As root, and before any Image Tool actions. |
| `package-manager-packages` | None | All | A list of OS packages, such as `ftp gzip`, separated by line or space. |
| `final-build-commands` | `AUXILIARY_IMAGE_PATH` `WDT_HOME` `WDT_MODEL_HOME`| Final image | After all Image Tool actions are complete, and just before the container image is finalized. |

Each section can contain one or more valid Dockerfile commands and would look like the following:

```dockerfile
[final-build-commands]
LABEL owner="middleware team"
COPY --chown=oracle:oracle files/my_additional_file.txt /auxiliary
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
final stage of the image build.

#### Using OpenShift

##### `--target`

The file permissions in the Auxiliary image should match the container image where WebLogic Server is installed.
The target option is supplied for Auxiliary images as a convenience to simplify creating images with the same owner:group file permissions.
Use the same value for `--target` when creating images with `create` and `createAuxImage`.

| Target | Default File Permissions | Default File Ownership |
| --- | --- | --- |
| `Default` | `rwxr-x---` | `oracle:oracle` |
| `OpenShift` | `rwxrwx---` | `oracle:root` |

#### Custom Base Images

##### `--fromImageProperties`

When specifying `--fromImage` to override the default base image, Image Tool needs additional information about the 
image that is being provided, such as the installed operating system and version.  By default, the additional information
is gathered automatically by the Image Tool using `docker run`.  If it is desirable to provide that additional information
manually and avoid the `docker run` step, `--fromImageProperties` must be provided with the additional information using 
a Java Properties file. The file must be a line-oriented format with key-value pairs separated by `=`.  For example:
```properties
packageManager=MICRODNF
__OS__ID="ol"
__OS__VERSION="8.10"
```
Required properties:

| Key | Description | Default |
| --- | --- | --- |
| `packageManager` | The name of the installed package manager in the `fromImage` in all CAPS.  Like `DNF`, `MICRODNF`, and `YUM` | `YUM` |
| `__OS__ID` | The ID value found in `/etc/os-release`.  Like "ol" for Oracle Linux, or "bb" for BusyBox. | |
| `__OS__VERSION` | The VERSION value found in `/etc/os-release`.  Like "8.10". | |

Additional properties:

| Key | Description |
| --- | --- |
| `javaHome` | The location where the JDK is pre-installed.  Like "/u01/jdk". |
| `__OS__arch` | The output of `uname -m`.  Like `amd64` or `arm64`. |


#### `--wdtVersion`

As of version 1.11.0, you may opt to install WDT and the model files in separate images.  By default, the cached `wdt_latest`
version of WDT is installed in the Auxiliary image with the selected models, archives, and variable files.  If you use
`--wdtVersion=none` (case insensitive), the auxiliary image will be created without installing WDT.  

#### Use an argument file

You can save all arguments passed for the Image Tool in a file, then use the file as a parameter.

For example, create a file called `build_args`:

```bash
createAuxImage
--tag mydomain:1
--wdtModel ./my_domain.yaml
--wdtArchive ./my_domain.zip
```

Use it on the command line, as follows:

```bash
$ imagetool @/path/to/build_args
```


### Usage scenarios

The following commands assume that the required WDT installer has been downloaded and added to the ImageTool cache.
Use the [cache]({{% relref "/userguide/tools/cache.md" %}}) command to set it up.

- Create an image named `wdt:1.10` with the latest [WebLogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/) version.
    ```bash
    $ imagetool createAuxImage --tag wdt:1.10
    ```

- Create an image named `mydomain:1` with the same options as above and add a WDT model and archive.
    ```bash
    $ imagetool create --tag mydomain:1 --wdtModel /path/to/my_domain.yaml --wdtArchive /path/to/my_domain.zip
    ```
