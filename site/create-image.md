# Create Image

The `create` command helps build a WebLogic Docker image from a given base OS image. The required options for the command
are marked with an asterisk (*). There are a number of optional parameters for the create feature.

```
Usage: imagetool create [OPTIONS]
Build WebLogic docker image
      --chown=<osUserAndGroup>[:<osUserAndGroup>...]
                    userid:groupid for JDK/Middleware installs and patches. Default:
                      oracle:oracle.
      --docker=<dockerPath>
                    path to docker executable. Default: docker
      --fromImage=<fromImage>
                    Docker image to use as base image.
      --httpProxyUrl=<httpProxyUrl>
                    proxy for http protocol. Ex: http://myproxy:80 or http://user:
                      passwd@myproxy:8080
      --httpsProxyUrl=<httpsProxyUrl>
                    proxy for https protocol. Ex: http://myproxy:80 or http://user:
                      passwd@myproxy:8080
      --installerResponseFile=<installerResponseFile>
                    path to a response file. Override the default responses for the
                      Oracle installer
      --jdkVersion=<jdkVersion>
                    Version of server jdk to install. Default: 8u202
      --latestPSU   Whether to apply patches from latest PSU.
      --opatchBugNumber=<opatchBugNumber>
                    use this opatch patch bug number
      --password=<password for support user id>
                    Password for support userId
      --passwordEnv=<environment variable>
                    environment variable containing the support password
      --passwordFile=<password file>
                    path to file containing just the password
      --patches=patchId[,patchId...]
                    Comma separated patch Ids. Ex: 12345678,87654321
*     --tag=TAG     Tag for the final build image. Ex: store/oracle/weblogic:
                      12.2.1.3.0
      --type=<installerType>
                    Installer type. Default: wls. Supported values: wls, fmw
      --user=<support email>
                    Oracle Support email id
      --version=<installerVersion>
                    Installer version. Default: 12.2.1.3.0
      --wdtArchive=<wdtArchivePath>
                    path to the WDT archive file used by the WDT model
      --wdtDomainHome=<wdtDomainHome>
                    pass to the -domain_home for wdt
      --wdtDomainType=<wdtDomainType>
                    WDT Domain Type. Default: WLS. Supported values: WLS, JRF,
                      RestrictedJRF
      --wdtModel=<wdtModelPath>
                    path to the WDT model file that defines the Domain to create
      --wdtRunRCU   instruct WDT to run RCU when creating the Domain
      --wdtVariables=<wdtVariablesPath>
                    path to the WDT variables file for use with the WDT model
      --wdtVersion=<wdtVersion>
                    WDT tool version to use
      --wdtBldProperties=<wdtBldProperties>
                    specify additional properties for WLST
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
