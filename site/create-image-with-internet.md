# Create an image with full Internet access

The Image Tool supports creating Linux-based WebLogic Docker images, applying WebLogic patches, and creating WebLogic domains.  It can be used with or without Internet access.

In this use case, the Image Tool will:

1. Start with a base-level operating system image (`oraclelinux:7-slim`).
2. Update the image with the necessary packages for installing WebLogic Server.
3. Install Java and WebLogic Server.

## Steps

1. Download these Java and WebLogic installers from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com)
and save them in a directory of your choice, for example, `/home/acmeuser/wls-installers`:

     `fmw_12.2.1.3.0_wls_Disk1_1of1.zip`\
     `jdk-8u202-linux-x64.tar.gz`


2. Use the [Cache Tool](cache.md) to add the installers:

    ```bash
    imagetool cache addInstaller --type jdk --version 8u202 --path /home/acmeuser/wls-installers/jdk-8u202-linux-x64.tar.gz
    ```

    ```bash
    imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    ```

    **Note**:  The value of the version must be a valid WebLogic Server version number. This version number is used to verify and find the correct patch file to download from Oracle Support.  The format of the version is a 5 digits tuple, separated by period.  For example,  ```12.2.1.3.0``` ```12.1.3.0.0```

3. Create the image using the [Create Tool](create-image.md) commands. For example:

  ```bash
  imagetool create --tag wls:12.2.1.3.0 --latestPSU --version 12.2.1.3.0 --user  username@mycompany.com --passwordEnv MYPWD  
  ```

   Where ```--user --passwordEnv``` provides the credentials for a user who is entitled to download patches from Oracle Support.

 **NOTE**: You can provide the password in one of the three ways:

 * Plain text
 * Environment variable
 * File containing the password


You will see the Docker command output as the tool runs:

```bash
[2019-05-28 10:37:02] [com.oracle.weblogic.imagetool.cli.menu.CreateImage] [INFO   ] tmp directory used for build
context: /home/acmeuser/wlsimgbuilder_temp8791654163579491583
[2019-05-28 10:37:09] [com.oracle.weblogic.imagetool.cli.menu.CreateImage] [INFO   ] docker cmd = docker build
--force-rm --rm=true --no-cache --tag wls:12.2.1.3.0 --build-arg http_proxy=http://company-proxy.com:80 --build-arg
https_proxy=http://company-proxy.com:80 --build-arg WLS_PKG=fmw_12.2.1.3.0_wls_Disk1_1of1.zip --build-arg
JAVA_PKG=jdk-8u201-linux-x64.tar.gz --build-arg PATCHDIR=patches /home/acmeuser/wlsimgbuilder_temp8791654163579491583
Sending build context to Docker daemon   1.08GB

Step 1/46 : ARG BASE_IMAGE=oraclelinux:7-slim
...
Removing intermediate container 57ccb9fff56b
 ---> 18d366fc3da4
Successfully built 18d366fc3da4
Successfully tagged wls:12.2.1.3.0
```

3. To verify that the image was created, use the `docker images` command:

```bash
docker images

REPOSITORY          TAG                 IMAGE ID            CREATED              SIZE
wls                 12.2.1.3.0          18d366fc3da4        About a minute ago   1.41GB
oraclelinux         7-slim              f7512ac13c1b        6 weeks ago          118MB

```
## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
