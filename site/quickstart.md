# Quick Start

The Image Tool supports creating WebLogic docker image, apply WebLogic patches, and creating WebLogic domains.  It 
can be used with or without internet access.  

Before you use the tool, you can customize the tool's cache store, the cache store is used to lookup where the Java, 
WebLogic installers and WebLogic patches reside in the local file system.

By default, it is stored in the user's ```$HOME/cache``` directory.  Under this directory, the lookup information is 
stored in the file ```.metadata```.  All automatically downloaded patches also reside in this directory.  

This default cache store location can be changed by running the ```setCacheDir``` command before using the cache store:

```bash
imagetool cache setCacheDir /path/to/dir
```

The high level steps for creating an image are:

1. Download the Java and WebLogic installers from Oracle Software Delivery Cloud.
2. Add the installers to the cache store.
3. Optionally download any WebLogic patches and add it to the cache store
4. Run the ```imagetool``` command to create or update an image

 
For example, you have saved the installers in /home/acmeuser/wls-installers as

fmw_12.2.1.3.0_wls_Disk1_1of1.zip\
jdk-8u202-linux-x64.tar.gz
 

Using the Cache Tool to add the installers:

```bash
imagetool cache addInstaller --type jdk --version 8u202 --path /home/acmeuser/wls-installers/jdk-8u202-linux-x64.tar.gz
```

```bash
imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
.tar.gz

```

Note:  The value of version must be a valid WebLogic version number. This version number is used to verify and 
find the correct patch file to download from Oracle Support.  The format of the version is a 5 digits tuple separated
 by period.  For example,  ```12.2.1.3.0``` ```12.1.3.0.0```

You can verify the cache entries by:

```bash
imagetool cache listItems
```

```bash
Cache contents
jdk_8u202=/home/acmeuser/wls-installers/jdk-8u202-linux-x64.tar.gz
wls_12.2.1.3.0=/home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip

```

During the image creation process, the tool creates a temporary directory prefixed by ```wlsimgbuilder_temp``` as 
the context root for ```docker build``` command.  This temporary directory will be deleted upon successful completion.
 
By default, it is created under the user's home directory. If you do not want to create the temporary directory under
 the home directory, you can set the environment variable first by:
 
 ```bash
export WLSIMG_BLDDIR=/path/to/dir
```

The final image has the following structure:

```bash
[oracle@c3fe8ee0167d oracle]$ ls -arlt /u01/
total 20
drwxr-xr-x  2 oracle oracle 4096 May 28 23:40 domains
drwxr-xr-x  7 oracle oracle 4096 May 28 23:40 jdk
drwxr-xr-x 11 oracle oracle 4096 May 28 23:40 oracle
drwxr-xr-x  5 oracle oracle 4096 May 28 23:40 .
drwxr-xr-x 18 root   root   4096 May 29 01:31 ..
```


## Creating image with full internet access

In this use case, the image tool will:

1. Start with a base level operating system image (oracle-linux:7-slim).
2. Automatically update the image with the necessary packages for installing WebLogic.
3. Install Java and WebLogic based on the provided installers.
4. Optionally automatically download and apply patches specified.
5. Optionally create a WebLogic domain with WebLogic Deploy Tool.


After you have downloaded the Java and WebLogic installers described before, you can create the image using the [Create 
Tool commands](create-image.md). For example:

```bash
imagetool create --tag wls:12.2.1.3.0 --latestPSU --version 12.2.1.3.0 --user username@mycompany.com --passwordEnv MYPWD  [--httpProxyUrl http://company-proxy:80 --httpsProxyUrl http://company-proxy:80]
```

where ```--user --passwordEnv``` provides the user credential who is entitled to download patches from Oracle Support

You will see the docker command output as the tool runs:

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

Once the image is created, you can use any docker commands on the image, for example:

```bash
docker images

REPOSITORY          TAG                 IMAGE ID            CREATED              SIZE
wls                 12.2.1.3.0          18d366fc3da4        About a minute ago   1.41GB
oraclelinux         7-slim              f7512ac13c1b        6 weeks ago          118MB

```

## Creating image with no internet access


In this use case, since there is no internet access.  You will be responsible for downloading all the installers and 
patches, plus setting up the cache.  You also have to provide a base operating system image that has the following packages 
installed.


```bash
gzip 
tar 
unzip
```

For each WebLogic patches, you will need to download it from Oracle Support and set up the cache. For example, if you downloaded patch number 
27342434 for WebLogic version 12.2.1.3.0:
 
```bash
imagetool cache addPatch --patchId 27342434 --version 12.2.1.3.0 --path /home/acmeuser/cache/p27342434_122130_Generic .zip -user username@mycompany.com --passwordEnv MYPWD
```

You need to provide the credential in the command line, it verifies the MD5 on the file system against the meta data on 
Oracle Support for this patch number

Then, you can run the command to create the image:

```bash
imagetool create --fromImage myosimg:latest --tag wls:12.2.1.3.0 --patches 27342434 --version 12.2.1.3.0 --useCache always
```

Sometimes, a WebLogic patch may require patching the OPatch binaries before applying them.  We recommend 
downloading the latest OPatch's patch and setup the cache.  For example, the latest OPatch's patch is 28186730, 13.9
.4.0.0.  You can use this command to setup the cache after downloading from Oracle Support.

```bash
imagetool cache addPatch --patchId 28186730 --version 13.9.4.0.0 --path /home/acmeuser/cache/p28186730_139400_Generic.zip
```

## Patching an existing image

This use case allows you apply WebLogic patches to an existing image. It works similar to previous use cases, you 
have the option to download automatically by the tool or manual downloading the patches yourself.

Once the cache is setup, you can use the following command to update an image:

```bash
imagetool update --fromImage wls:12.2.1.3.0 --tag wls:12.2.1.3.4 --patches 27342434 --version 12.2.1.3.0 --useCache always
```

## Create an image with a WebLogic Domain using WebLogic Deploy Tool

The image tool allows create a customized WebLogic domain using [WebLogic Deploy Tool](https://github.com/oracle/weblogic-deploy-tooling).  

You can accomplish this by:

Download the [WebLogic Deploy Tool from](https://github.com/oracle/weblogic-deploy-tooling/releases), then add it to the cache store

```bash
imagetool cache addInstaller --type wdt --version 2.2 --path /home/acmeuser/cache/weblogic-deploy.zip
```

Provide the command line options for WebLogic Deploy Tool

```bash
imagetool create --fromImage myosimg:latest --tag wls:12.2.1.3.0 --patches 27342434 --version 12.2.1.3.0 --useCache always --wdtVersion 2.2 --wdtArchive /home/acmeuser/wdt/domain1.zip --wdtDomainHome /u01/domains/simple_domain
```

The parameters mapping between Image Tool and WebLogic Deploy Tool are:

| Image Tool         | WebLogic Deploy Tool |
|--------------------|-------------------------|
| --wdtArchive       | -archive_file           |
| --wdtModel         | -model_file             |
| --wdtVariables     | -variable_file          |
| --run_rcu          | -run_rcu                |
| --wdtDomainHome    | -domain_home            |


The domain will be created under ```/u01/domains/base_domain``` if you do not specify ```--wdtDomainHome```.  

Note: if you are creating a JRF domain and wants WebLogic Deploying Tool to create the rcu schemas for you, you can specify the connection info in the model [Specifying RCU 
information in the model](https://github.com/oracle/weblogic-deploy-tooling/blob/master/site/rcuinfo.md)


## Using an argument file

All arguments passed for the Image Tool can be saved in a file and using the file as parameter. For example:

Create a file called build_args:

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

and use it in the command line as:

```bash
imagetool @/path/to/build_args
```


## Cleanup

As described in the begging, the image tool creates a temporary directory prefixed by ```wlsimgbuilder_temp``` 
every time it runs.  This directory will be deleted under normal circumstances, however if the process is aborted, 
the directory needs to be deleted manually.
 
If you see dangling images after the build. You can use the following commands to remove them:

```bash
docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
```

## Logging

In a rare circumstance, it may be helpful to turn on debugging to reveal more information.  The tool use the standard
logging.properties file under the tool's ```bin``` directory. 


## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
