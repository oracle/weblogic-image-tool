# Create Image

The `create` command helps build a WebLogic Docker image from a given base OS image. The required option for the command is marked. There are a number of optional parameters for the create feature.

```
Usage: imagetool create [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
|`--additionalBuildCommands`| Path to a file with additional build commands. For more details, see [Additional information](#additional_information). |
|`--chown` | `userid:groupid` for JDK/Middleware installs and patches.  | `oracle:oracle` |
| `--docker` | Path to the Docker executable.  |  `docker` |
| `--fromImage` | Docker image to use as a base image. |   |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--installerResponseFile` | Path to a response file. Overrides the default responses for the Oracle installer.  |   |
| `--jdkVersion` | Version of the server JDK to install.  | `8u202`  |
| `--latestPSU` | Whether to apply patches from the latest PSU.  |   |
| `--opatchBugNumber` | The patch number for OPatch (patching OPatch).  |   |
| `--password` | Password for the support `userId`.  |   |
| `--passwordEnv` | Environment variable containing the support password.  |   |
| `--passwordFile` | Path to a file containing just the password.  |   |
| `--patches` | Comma separated list of patch IDs. Example: `12345678,87654321`  |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0`  |   |
| `--type` | Installer type. Supported values: `WLS`, `FMW`  | `WLS`  |
| `--user` | Oracle support email ID.  |   |
| `--version` | Installer version. | `12.2.1.3.0`  |
| `--wdtArchive` | Path to the WDT archive file used by the WDT model.  |   |
| `--wdtDomainHome` | Path to the `-domain_home` for WDT.  |   |
| `--wdtDomainType` | WDT domain type. Supported values: `WLS`, `JRF`, `RestrictedJRF`  | `WLS`  |
| `--wdtJavaOptions` | Java command-line options for WDT.  |   |
| `--wdtModel` | Path to the WDT model file that defines the domain to create.  |   |
| `--wdtModelOnly` | Install WDT and copy the models to the image, but do not create the domain.  | `false`  |
| `--wdtRunRCU` | Instruct WDT to run RCU when creating the domain.  |   |
| `--wdtStrictValidation` | Use strict validation for the WDT validation method. Only applies when using model only.  | `false`  |
| `--wdtVariables` | Path to the WDT variables file for use with the WDT model.  |   |
| `--wdtVersion` | WDT tool version to use.  |   |

## Additional information
This section provides additional information for command-line parameters requiring more details or clarification.

#### `additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections: `before-jdk-install`, `after-jdk-install`, `before-fmw-install`, `after-fmw-install`, `before-wdt-command`, `after-wdt-command`, `final-build-commands`.

Each section can contain one or more valid Dockerfile commands and would look like the following:

```dockerfile
[after-fmw-install]
RUN rm /some/dir/unnecessary-file

[final-build-commands]
LABEL owner="middleware team"
```


## Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

The commands below assume that all the required JDK, WLS, or FMW (WebLogic infrastructure installers) have been downloaded
 to the cache directory. Use the [cache](cache.md) command to set it up.

- Create an image named `sample:wls` with the WebLogic installer 12.2.1.3.0, server JDK 8u202, and latest PSU applied.
    ```
    imagetool create --tag sample:wls --latestPSU --user testuser@xyz.com --password hello
    ```

- Create an image named `sample:wdt` with the same options as above and create a domain with [WebLogic Deploy Tooling](https://github.com/oracle/weblogic-deploy-tooling).
    ```
    imagetool create --tag sample:wdt --latestPSU --user testuser@xyz.com --password hello --wdtModel /path/to/model.json --wdtVariables /path/to/variables.json --wdtVersion 0.16
    ```
    If `wdtVersion` is not provided, the tool uses the latest release.

- Create an image named `sample:patch` with the selected patches applied.
    ```
    imagetool create --tag sample:patch --user testuser@xyz.com --password hello --patches 12345678,p87654321
    ```
    The patch numbers may or may not start with '`p`'.

## Errors

- `CachePolicy prohibits download. Please add cache entry for key: jdk_8u202`

   - This implies that the tool could not find the JDK installer in its cache.
   - Use the [cache](cache.md) command to fix it:
    ```
    imagetool cache addInstaller --type jdk --version 8u202 --path /local/path/to/jdk.gz
    ```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
