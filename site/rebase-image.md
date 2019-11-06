# Rebase Image

The `rebase` command creates a new Docker image and copy a WebLogic Domain to the image.  The new Docker image can be based on an existing image in the repository or creating a new one similar to the `create` command.

```
Usage: imagetool rebase [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
|`--additionalBuildCommands`| Path to a file with additional build commands. For more details, see [Additional information](#additional-information). |
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
| `--sourceImage` | Source Image containing the WebLogic Domain. |   |
| `--targetImage` | Target Image. |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0`  |   |
| `--type` | Installer type. Supported values: `WLS`, `FMW`  | `WLS`  |
| `--user` | Oracle support email ID.  |   |
| `--version` | Installer version. | `12.2.1.3.0`  |

## Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections: `before-jdk-install`, `after-jdk-install`, `before-fmw-install`, `after-fmw-install`, `before-wdt-command`, `after-wdt-command`, `final-build-commands`.

Each section can contain one or more valid Dockerfile commands and would look like the following:

```dockerfile
[after-fmw-install]
RUN rm /some/dir/unnecessary-file

[final-build-commands]
LABEL owner="middleware team"
```

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
imagetool @/path/to/build_args
```


## Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

The commands below assume that all the required JDK, WLS, or FMW (WebLogic infrastructure installers) have been downloaded
 to the cache directory. Use the [cache](cache.md) command to set it up.


- Rebase a source image named `sample:v1` using a target image named `fmw:12214` 
    ```
    imagetool rebase --tag sample:v2 --sourceImage sample:v1 --targetImage fmw:12214
    ```

- Creaet a new image based on WebLogic version 12.2.1.3.0 with patches but rebase a source image named `sample:v1` 
    ```
    imagetool rebase --tag sample:v2 --user testuser@xyz.com --password hello --patches 12345678,87654321 --sourceImage sample:v1
    ```

## Errors

- `CachePolicy prohibits download. Please add cache entry for key: jdk_8u202`

   - This implies that the tool could not find the JDK installer in its cache.
   - Use the [cache](cache.md) command to fix it:
    ```
    imagetool cache addInstaller --type jdk --version 8u202 --path /local/path/to/jdk.gz
    ```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates.  All rights reserved.
