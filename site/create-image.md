# Create Image

The create command helps build a WebLogic docker image from a given base os image. The required options for the command 
are marked with asterisk (*). Password can be provided in one of the three ways. 
1) Plain Text
2) Environment Variable
3) File containing the password

```
Usage: imagebuilder create [OPTIONS]
Build WebLogic docker image
      --docker=<dockerPath> path to docker executable. Default: docker
      --fromImage=<fromImage>
                            Docker image to use as base image.
      --httpProxyUrl=<httpProxyUrl>
                            proxy for http protocol. Ex: http://myproxy:80 or http:
                              //user:passwd@myproxy:8080
      --httpsProxyUrl=<httpsProxyUrl>
                            proxy for https protocol. Ex: http://myproxy:80 or http:
                              //user:passwd@myproxy:8080
      --jdkVersion=<jdkVersion>
                            Version of server jdk to install. default: 8u202
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
      --type=<installerType>
                            Installer type. default: wls. supported values: wls, fmw
*     --user=<support email>
                            Oracle Support email id
      --version=<installerVersion>
                            Installer version. default: 12.2.1.3.0
      --wdtArchive=<wdtArchivePath>
                            path to wdt archive file used by wdt model
      --wdtModel=<wdtModelPath>
                            path to the wdt model file to create domain with
      --wdtVariables=<wdtVariablesPath>
                            path to wdt variables file used by wdt model
      --wdtVersion=<wdtVersion>
                            wdt version to create the domain
```

## Usage scenarios

**_Note: Use --passwordEnv or --passwordFile instead of --password_**

The commands below assume that all the required jdk, wls or fmw (WebLogic infrastructure installer) have been downloaded
 to the cache dir. Use the [cache](cache.md) command to set it up.

- Create an image named sample:wls with WebLogic installer 12.2.1.3.0, server jdk 8u202, latest psu applied.
    ```
    imagebuilder create --tag sample:wls --latestPSU --user testuser@xyz.com --password hello
    ```

- Create an image named sample:wdt with same options as above and create a domain with [WebLogic Deploy Tooling](https://github.com/oracle/weblogic-deploy-tooling)
    ```
    imagebuilder create --tag sample:wdt --latestPSU --user testuser@xyz.com --password hello --wdtModel /path/to/model.json --wdtVariables /path/to/variables.json --wdtVersion 0.16
    ```
    If wdtVersion is not provided, the tool uses the latest release.

- Create an image name sample:patch with selected patches applied.
    ```
    imagebuilder create --tag sample:patch --user testuser@xyz.com --password hello --patches 12345678,p87654321
    ```
    The patch numbers may or may not start with 'p'.
    
## Errors

- CachePolicy prohibits download. Please add cache entry for key: jdk_8u202
    - This implies that the tool could not find the jdk installer in its cache. Use the [cache](cache.md) command to fix it.
    ```
    imagebuilder cache addInstaller --type jdk --version 8u202 --path /local/path/to/jdk.gz
    ```