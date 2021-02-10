# Update Image

After you have created a Docker image with the Image Tool, you may want to change it from time to time.  Use the `update`
command to update the existing Docker images created with the Image Tool.  For example, you may want to:
* Apply a WebLogic patch
* Apply the latest PSU from Oracle
* Create a new WebLogic domain (if one did not already exist)
* Deploy a new application to an existing domain
* Modify the domain configuration (add a data source, change a port number, and such)

The required options for the `update` command are marked.

**NOTE**: You can provide the password in one of the three ways:

* Plain text
* Environment variable
* File containing the password

```
Usage: imagetool update [OPTIONS]
Update WebLogic Docker image with selected patches

```
| Parameter | Definition | Default |
| --- | --- | --- |
| `--additionalBuildCommands` | Path to a file with additional build commands. For more details, see [Additional information](#additional-information). |
| `--additionalBuildFiles` | Additional files that are required by your `additionalBuildCommands`.  A comma separated list of files that should be copied to the build context. |
| `--buildNetwork` | Networking mode for the RUN instructions during the image build.  See `--network` for Docker `build`.  |   |
| `--chown` | `userid:groupid` for JDK/Middleware installs and patches.  | `oracle:oracle` |
| `--docker` | Path to the Docker executable.  |  `docker` |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout.  |  |
| `--fromImage` | Docker image to be updated. The `fromImage` option serves as a starting point for the new image to be created. | `weblogic:12.2.1.3.0`  |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080`  |   |
| `--latestPSU` | (DEPRECATED) Find and apply the latest PatchSet Update, see [Additional information](#additional-information).  |   |
| `--opatchBugNumber` | The patch number for OPatch (patching OPatch).  |   |
| `--password` | Request password for the Oracle Support `--user` on STDIN, see `--user`.  |   |
| `--passwordEnv` | Environment variable containing the Oracle Support password, see `--user`.  |   |
| `--passwordFile` | Path to a file containing just the Oracle Support password, see `--user`.  |   |
| `--patches` | Comma separated list of patch IDs. Example: `12345678,87654321`  |   |
| `--pull` | Always attempt to pull a newer version of base images during the build.  |   |
| `--resourceTemplates` | One or more files containing placeholders that need to be resolved by the Image Tool. See [Resource Template Files](#resource-template-files). |   |
| `--strictPatchOrdering` |  Instruct OPatch to apply patches one at a time (uses `apply` instead of `napply`). |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/weblogic:12.2.1.3.0`  |   |
| `--user` | Oracle support email ID.  |   |
| `--wdtArchive` | Path to the WDT archive file used by the WDT model.  |   |
| `--wdtDomainHome` | Path to the `-domain_home` for WDT.  |   |
| `--wdtDomainType` | WDT domain type. Supported values: `WLS`, `JRF`, `RestrictedJRF`  | `WLS`  |
| `--wdtEncryptionKey` | Passphrase for WDT -use_encryption that will be requested on STDIN. |   |
| `--wdtEncryptionKeyEnv` | Passphrase for WDT -use_encryption that is provided as an environment variable. |   |
| `--wdtEncryptionKeyFile` | Passphrase for WDT -use_encryption that is provided as a file. |   |
| `--wdtJavaOptions` | Java command-line options for WDT.  |   |
| `--wdtModel` | Path to the WDT model file that defines the domain to create.  |   |
| `--wdtModelOnly` | Install WDT and copy the models to the image, but do not create the domain.  | `false`  |
| `--wdtOperation` | Create a new domain, or update an existing domain. Supported values: `CREATE`, `UPDATE`, `DEPLOY`  | `CREATE`  |
| `--wdtRunRCU` | Instruct WDT to run RCU when creating the domain.  |   |
| `--wdtStrictValidation` | Use strict validation for the WDT validation method. Only applies when using model only.  | `false`  |
| `--wdtVariables` | Path to the WDT variables file for use with the WDT model.  |   |
| `--wdtVersion` | WDT tool version to use.  |   |

## Additional information

#### `--additionalBuildCommands`

This is an advanced option that let's you provide additional commands to the Docker build step.  
The input for this parameter is a simple text file that contains one or more of the valid sections. Valid sections for update:

| Section | Build Stage | Timing |
| --- | --- | --- |
| `before-wdt-command` | Intermediate (WDT_BUILD) | Before WDT is installed. |
| `after-wdt-command` | Intermediate (WDT_BUILD) | After WDT domain creation/update is complete. |
| `final-build-commands` | Final image | After all Image Tool actions are complete, and just before the Docker image is finalized. |

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

The `latestPSU` option will continue to be supported for the CREATE option, but has been deprecated for use in the 
UPDATE option.  Because of the number of patches and their size, using `latestPSU` as an update to an existing image can 
increase the size of the image significantly, and is not recommended. 

#### Resource Template Files

If provided, the file or files provided with `--resourceTemplates` will be overwritten. For known tokens, 
the placeholders will be replaced with values according to the following table.  **Note:** Placeholders must follow
the Mustache syntax, like `{{imageName}}` or `{{{imageName}}}`.

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
imagetool @/path/to/build_args
```


## Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

- Update an image named `sample:1.0` by applying the latest PSU and tag it as `sample:1.1`.
    ```
    imagetool update --fromImage sample:1.0 --tag sample:1.1 --latestPSU --user test@xyz.com --passwordEnv MYVAR
    ```

- Update an image named `sample:1.0` with the selected patches applied and tag it as `sample:1.1`.
    ```
    imagetool update --fromImage sample:1.0 --tag sample:1.1 --user test@xyz.com --password hello --patches 12345678,87654321
    ```

- Update an image named `wls:12.2.1.3.0` by creating a new WebLogic domain using WDT and tag it as `mydomain:1`.  The WDT
installer is accessed from the cache with key wdt_1.1.1.  The model and archive to use are located in a subfolder named `wdt`.
    ```
    imagetool update --fromImage wls:12.2.1.3.0 --tag mydomain:1 --wdtArchive ./wdt/my_domain.zip --wdtModel ./wdt/my_domain.yaml --wdtVersion 1.1.1
    ```

- Use `deployApps` from WDT to deploy a new application, the WLS Metrics Exporter.  The model and archive to use are
located in a subfolder named `wdt`.
    ```
    imagetool update --tag mydomain:2 --fromImage mydomain:1 --wdtOperation deploy --wdtArchive ./wdt/exporter_archive.zip --wdtModel ./wdt/exporter_model.yaml --wdtVersion 1.1.1   
    ```

## Copyright
Copyright (c) 2019, 2021, Oracle and/or its affiliates.
