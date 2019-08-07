# Update Image

Use the `update` command to apply patches to an existing WebLogic Docker image. The required options for the command
are marked with an asterisk (*). The password can be provided in one of the three ways:

* Plain text
* Environment variable
* File containing the password

```
Usage: imagetool update [OPTIONS]
Update WebLogic docker image with selected patches
      --docker=<dockerPath> path to docker executable. Default: docker
*     --fromImage=<fromImage>
                            Docker image to use as base image.
      --httpProxyUrl=<httpProxyUrl>
                            proxy for http protocol. Ex: http://myproxy:80 or http:
                              //user:passwd@myproxy:8080
      --httpsProxyUrl=<httpsProxyUrl>
                            proxy for https protocol. Ex: http://myproxy:80 or http:
                              //user:passwd@myproxy:8080
      --latestPSU           Whether to apply patches from latest PSU.
      --password=<password for support user id>
                            Password for support userId
      --passwordEnv=<environment variable>
                            environment variable containing the support password
      --passwordFile=<password file>
                            path to file containing just the password
      --patches=patchId[,patchId...]
                            Comma separated patch Ids. Ex: p12345678,p87654321
*     --tag=TAG             Tag for the final build image. Ex: store/oracle/weblogic:
                              12.2.1.3.0
*     --user=<support email>
                            Oracle Support email id
      --wdtArchive=<wdtArchivePath>
                            path to wdt archive file used by wdt model
      --wdtModel=<wdtModelPath>
                            path to the wdt model file to create domain with
      --wdtDomainHome=<wdtDomainHome>
                            path to the domain home for create domain
      --wdtDomainType=<wdtDomainType>
                            type of the domain to create                           
      --wdtVariables=<wdtVariablesPath>
                            path to wdt variables file used by wdt model
      --wdtVersion=<wdtVersion>
                            wdt version to create the domain
      --wdtBldProperties=<wdtBldProperties>
                            specify additional properties for WLST                            
```

## Usage scenarios

**Note**: Use `--passwordEnv` or `--passwordFile` instead of `--password`.

- Update an image named `sample:1.0` by applying the latest PSU and tag it as `sample:1.1`.
    ```
    imagetool update --fromImage sample:1.0 --tag sample:1.1 --latestPSU --user test@xyz.com --passwordEnv MYVAR
    ```

- Update an image named `sample:1.0` with the selected patches applied.
    ```
    imagetool update --fromImage sample:1.0 --tag sample:1.1 --user test@xyz.com --password hello --patches 12345678,87654321
    ```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
