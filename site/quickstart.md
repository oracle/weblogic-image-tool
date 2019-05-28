# Quick Start

The Image Tool supports creating WebLogic docker images with or without internet access. 

In all cases, you are required to download the Java and WebLogic installers manually and setup the cache for the file 
locations.  You have the option of specifying one or more WebLogic patches to be applied and whether to let the tool to 
automatically download for you.

After downloading the installers for WebLogic and Java, you will need to setup the cache tool to specify where the
 installers reside.  The cache items acted as a lookup to where each entry is stored and also storing any 
 automactially downloaded WebLogic pataches.

By default, it is stored in the user's $HOME/cache directory.  Under this directory, the lookup table is stored in 
the file .metadata properties file and any downloaded patch will be stored in the same directory as well.  This behavior
 can be changed by first running the cache setCacheDir command:

```aidl
imagetool cache setCacheDir /path/to/dir
```

After setting up the cache store, you can use the cache store to store installers and patches.
 
For example, you have saved the installers in /home/aimeuser/oracle-installers as

fmw_12.2.1.3.0_wls_Disk1_1of1.zip\
jdk-8u202-linux-x64.tar.gz
 

Using the Cache Tool to add the installers:

```aidl
imagetool cache addInstaller --type jdk --version 8u202 --path /home/aimeuser/oracle-installers/jdk-8u202-linux-x64.tar.gz
```

```aidl
imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /home/aimeuser/oracle-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
.tar.gz

```

Note:  the value of version must be a valid WebLogic version number. The version number is used to download the 
correct version of any patches based on this number.  The format of the version is a 5 digits tuple separated by period.

You can verify the cache entries by:

```aidl
imagetool cache listItems
```

```aidl
Cache contents
jdk_8u202=/home/aimeuser/oracle-installers/jdk-8u202-linux-x64.tar.gz
wls_12.2.1.3.0=/home/aimeuser/oracle-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip

```


## Creating image with full internet access

In this use case, the image tool will:

1. Start with a base level operating system image (default is oracle-linux:7-slim).
2. Automatically update the image with the necessary packages for installing WebLogic.
3. Install Java and WebLogic based on the provided installers.
4. Optionally automatically download and apply patches specified.
5. Optionally create a WebLogic domain with WebLogic Deploying Tool.


Once the cache is setup, you can create the image using the [Create Tool commands](create-image.md), for example:

```aidl
imagetool create --tag wls:12.2.1.3.0 --latestPSU --version 12.2.1.3.0 --user username@mycompany.com --passwordEnv MYPWD  [--httpProxyUrl http://company-proxy:80 --httpsProxyUrl http://company-proxy:80]
```

where MYPWD is the password for the support user entitled to download patches from Oracle Support Site.

You will see the docker command output as the tool runs:

```aidl
[2019-05-28 10:37:02] [com.oracle.weblogic.imagetool.cli.menu.CreateImage] [INFO   ] tmp directory used for build 
context: /home/aimeuser/wlsimgbuilder_temp8791654163579491583 
[2019-05-28 10:37:09] [com.oracle.weblogic.imagetool.cli.menu.CreateImage] [INFO   ] docker cmd = docker build 
--force-rm --rm=true --no-cache --tag wls:12.2.1.3.0 --build-arg http_proxy=http://company-proxy.com:80 --build-arg 
https_proxy=http://company-proxy.com:80 --build-arg WLS_PKG=fmw_12.2.1.3.0_wls_Disk1_1of1.zip --build-arg 
JAVA_PKG=jdk-8u201-linux-x64.tar.gz --build-arg PATCHDIR=patches /home/aimeuser/wlsimgbuilder_temp8791654163579491583 
Sending build context to Docker daemon   1.08GB

Step 1/46 : ARG BASE_IMAGE=oraclelinux:7-slim
...
Removing intermediate container 57ccb9fff56b
 ---> 18d366fc3da4
Successfully built 18d366fc3da4
Successfully tagged wls:12.2.1.3.0
```

Once the image is created, you can use any docker commands on the image, for example:

```aidl
docker images

REPOSITORY          TAG                 IMAGE ID            CREATED              SIZE
wls                 12.2.1.3.0          18d366fc3da4        About a minute ago   1.41GB
<none>              <none>              7289d2dd6a03        2 minutes ago        2.31GB
<none>              <none>              22e4c9ce822b        5 minutes ago        717MB
oraclelinux         7-slim              f7512ac13c1b        6 weeks ago          118MB

```

## Creating image with no internet access


In this use case, since there is no internet access.  You will be responsible for downloading all the installers and 
patches, setup the cache as well as providing a base image that has the following packages installed.


```aidl
gzip 
tar 
unzip
```

First, you download the installers and setup the cache as described earlier.  Then, for each WebLogic patches, you 
will need to download it from Oracle Support and set up the cache. For example, if you downloaded patch number 
27342434 for WebLogic version 12.2.1.3.0:
 
```aidl
imagetool cache addPatch --patchId 27342434 --version 12.2.1.3.0 --path /home/aimeuser/oracle-patches/p27342434_122130_Generic.zip
```

Once all the installers and patches are downloaded and setup in the cache, you can run the command to create the image:

```aidl
imagetool create --fromImage myosimg:latest --tag wls:12.2.1.3.0 --patches 27342434 --version 12.2.1.3.0 --useCache always
```

Sometimes, a WebLogic patch may require patching the OPatch binaries before applying them.  We recommend 
downloading the latest OPatch's patch and setup the cache.  For example, the latest OPatch's patch is 28186730, 13.9
.4.0.0.  You can use this command to setup the cache after downloading from the Oracle Support.

```aidl
imagetool cache addPatch --patchId 28186730 --version 13.9.4.0.0 --path 
/home/aimeuser/oracle-patches/p28186730_139400_Generic.zip

```

## Patching an existing image

This use case allows you apply WebLogic patches to an existing image. It works similar to previous use cases, you 
have the option to download automatically by the tool or manual downloading the patches yourself.

Once the cache is setup, you can use the following command to update an image:

```aidl
imagetool update --fromImage wls:12.2.1.3.0 --tag wls:12.2.1.3.4 --patches 27342434 --version 12.2.1.3.0 --useCache always
```

## Cleanup

During image creation, it creates a temporary directory under the user's home prefixed as wlsimgbuilder_tempXXXXXX 
where XXXXXX is a random number.  This directory will be deleted under normal circumstances, however if you aborted the process, the directory needs to
 be cleaned up manually.

## Logging

In a rare circumstance, it may be helpful to turn on debugging to reveal more information.  The tool use the standard
logging.properties file under the bin directory. 


## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
