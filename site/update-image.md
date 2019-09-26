# Update Image

After you have created a Docker image with the Image Tool, you may want to change it from time to time.  The `update` 
command should be used to update these existing Docker images created with the Image Tool.  For example, you may want to:
* Apply a WebLogic patch
* Apply the latest PSU from Oracle 
* Create a new WebLogic domain (if one did not already exist) 
* Deploy a new application to an existing domain
* Modify the domain configuration (add a data source, change a port number, ...)
 
The required options for the `update` command are marked with an asterisk (*), below. 
**NOTE:** The password can be provided in one of the three ways:

* Plain text
* Environment variable
* File containing the password

```
Usage: imagetool update [OPTIONS]
Update WebLogic docker image with selected patches
      --additionalBuildCommands=<additionalBuildCommandsPath>
                       path to a file with additional build commands
      --chown=<osUserAndGroup>[:<osUserAndGroup>...]
                       userid:groupid for JDK/Middleware installs and patches.
                         Default: oracle:oracle.
      --docker=<dockerPath>
                       path to docker executable. Default: docker
*     --fromImage=<fromImage>
                       Docker image to use as base image.
      --httpProxyUrl=<httpProxyUrl>
                       proxy for http protocol. Ex: http://myproxy:80 or http://user:
                         passwd@myproxy:8080
      --httpsProxyUrl=<httpsProxyUrl>
                       proxy for https protocol. Ex: http://myproxy:80 or http:
                         //user:passwd@myproxy:8080
      --latestPSU      Whether to apply patches from latest PSU.
      --opatchBugNumber=<opatchBugNumber>
                       the patch number for OPatch (patching OPatch)
      --password=<password for support user id>
                       Password for support userId
      --passwordEnv=<environment variable>
                       environment variable containing the support password
      --passwordFile=<password file>
                       path to file containing just the password
      --patches=patchId[,patchId...]
                       Comma separated patch Ids. Ex: 12345678,87654321
*     --tag=TAG        Tag for the final build image. Ex: store/oracle/weblogic:
                         12.2.1.3.0
      --user=<support email>
                       Oracle Support email id
      --wdtArchive=<wdtArchivePath>
                       path to the WDT archive file used by the WDT model
      --wdtDomainHome=<wdtDomainHome>
                       pass to the -domain_home for wdt
      --wdtDomainType=<wdtDomainType>
                       WDT Domain Type. Default: WLS. Supported values: WLS, JRF,
                         RestrictedJRF
      --wdtJavaOptions=<wdtJavaOptions>
                       Java command line options for WDT
      --wdtModel=<wdtModelPath>
                       path to the WDT model file that defines the Domain to create
      --wdtModelOnly   Install WDT and copy the models to the image, but do not
                         create the domain. Default: false.
      --wdtOperation=<wdtOperation>
                       Create a new domain, or update an existing domain.  Default:
                         CREATE. Supported values: CREATE, UPDATE, DEPLOY
      --wdtRunRCU      instruct WDT to run RCU when creating the Domain
      --wdtStrictValidation
                       Use strict validation for the WDT validation method. Only
                         applies when using model only.  Default: false.
      --wdtVariables=<wdtVariablesPath>
                       path to the WDT variables file for use with the WDT model
      --wdtVersion=<wdtVersion>
                       WDT tool version to use
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
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
