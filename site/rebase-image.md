# Rebase Image

The `rebase` command creates a new Docker image and copies an existing WebLogic domain to that new image.  
The new Docker image can be based on an existing image in the repository or created as part of the rebase operation 
similar to the `create` command.

```
Usage: imagetool rebase [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
| `--additionalBuildCommands` | Path to a file with additional build commands. For more details, see [Additional information](#additional-information). |
| `--additionalBuildFiles` | Additional files that are required by your `additionalBuildCommands`.  A comma separated list of files that should be copied to the build context. |
| `--chown` | `userid:groupid` for JDK/Middleware installs and patches.  | `oracle:oracle` |
| `--docker` | Path to the Docker executable.  |  `docker` |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout.  |  |
| `--fromImage` | Docker image to use as a base image when creating a new image. | `oraclelinux:7-slim`  |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080`  |   |
| `--installerResponseFile` | One or more custom response files. A comma separated list of paths to installer response files. Overrides the default responses for the Oracle silent installer.  |   |
| `--inventoryPointerFile` | Path to custom inventory pointer file.  |   |
| `--inventoryPointerInstallLoc` | Target location for the inventory pointer file.  |   |
| `--jdkVersion` | Version of the server JDK to install.  | `8u202`  |
| `--latestPSU` | Find and apply the latest PatchSet Update.  |   |
| `--opatchBugNumber` | The patch number for OPatch (patching OPatch).  |   |
| `--password` | Request password for the Oracle Support `--user` on STDIN, see `--user`.  |   |
| `--passwordEnv` | Environment variable containing the Oracle Support password, see `--user`.  |   |
| `--passwordFile` | Path to a file containing just the Oracle Support password, see `--user`.  |   |
| `--patches` | Comma separated list of patch IDs. Example: `12345678,87654321`  |   |
| `--sourceImage` | (Required) Source Image containing the WebLogic domain. |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0`  |   |
| `--targetImage` | Docker image to extend for the domain's new image. |   |
| `--type` | Installer type. Supported values: `WLS`, `FMW`, `BI`, `ODI`, `OHSSA`, `OSB`, `OUDSM`, `SOAOSB`, `WCP`, `EDQ`, `OAM`, `OHS`, `OIG`, `OUD`, `SOA`, `WCC`, `WCS`  | `WLS`  |
| `--user` | Your Oracle support email ID.  |   |
| `--version` | Installer version. | `12.2.1.3.0`  |

## Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections: `before-jdk-install`, `after-jdk-install`, `before-fmw-install`, `after-fmw-install`, `before-wdt-command`, `after-wdt-command`, `final-build-commands`.

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
All provided files are copied directly under the `files` subfolder of the build context.  
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

#### Use an argument file

You can save all arguments passed for the Image Tool in a file, then use the file as a parameter.

For example, create a file called `build_args`:

```bash
rebase
--tag wls:122140
--sourceImage wls:122130
--version 12.2.1.4.0
--jdkVersion 8u221
```

Use it on the command line, as follows:

```bash
imagetool @/path/to/build_args
```


## Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

The commands below assume that all the required JDK, WLS, or FMW (WebLogic infrastructure installers) have been downloaded
 to the cache directory. Use the [cache](cache.md) command to set it up.


- Update the JDK for an existing domain.  Copy the existing domain from `sample:v1` where the JDK was 8u202 to a new 
image called `sample:v2` and install the newer JDK 8u221 with WebLogic Server 12.2.1.3.0.
    ```
    imagetool rebase --tag sample:v2 --sourceImage sample:v1 --version 12.2.1.3.0 --jdkVersion 8u221 
    ```

- Update the Oracle Home for an existing domain with a newer WebLogic version.  Copy a domain from an existing image to 
a new image with a new install of WebLogic Server 12.2.1.4.0.  Copy the domain 
from `sample:v1` and select the desired WebLogic installer using the `--version` argument.  
    ```
    imagetool rebase --tag sample:v2 --sourceImage sample:v1 --version 12.2.1.4.0 --jdkVersion 8u221 
    ```

- Update the JDK and/or Oracle Home for an existing domain using another image with pre-installed binaries. 
Copy the domain from the source image named `sample:v1` to a new image called `sample:v2` based on a target image 
named `fmw:12214`.  **Note**: The Oracle Home and JDK must be installed in the same same folders on each image.
    ```
    imagetool rebase --tag sample:v2 --sourceImage sample:v1 --targetImage fmw:12214
    ```


## Copyright
Copyright (c) 2019, 2020, Oracle and/or its affiliates.  All rights reserved.
