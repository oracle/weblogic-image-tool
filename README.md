# Oracle WebLogic Image Tool

Oracle is finding ways for organizations using WebLogic Server to run important workloads, to move those workloads into
the cloud, and to simplify and speed up the application deployment life cycle. By adopting industry standards, such as Docker
and Kubernetes, WebLogic now runs in a cloud neutral infrastructure.  To help simplify and automate the creation of
Docker images for WebLogic Server, we are providing this open-source
Oracle WebLogic Image Tool.  This tool let's you create a new image, with installations of a JDK and WebLogic Server,
and optionally, configure a WebLogic domain with your applications, apply WebLogic Server patches, or update an existing
image.

## Features

The Image Tool provides three functions within the main script:
  - [Create Image](site/create-image.md) - The `create` command helps build a WebLogic Docker image from a given base OS
  image.
  - [Update Image](site/update-image.md) - The `update` command can be used to apply WebLogic patches to an existing
  WebLogic Docker image.
  - [Cache](site/cache.md) - The Image Tool maintains a local file cache for patches and installers.  The `cache`
  command can be used to manipulate the local file cache.

## Prerequisites

- Docker client and daemon on the build machine, with minimum Docker version 17.05+ to support multi-stage builds.
- WebLogic Server and JDK installers from OTN / Oracle e-Delivery.
- (For patches) Oracle support credentials.
- Bash version 4.0 or higher to enable the `<tab>` command complete feature.

## Setup

- Build the project (`mvn clean package`), to create the ZIP installer in ./imagetool/target.
- Unzip the release ZIP file to a desired location.
- If running an OS with bash, run `cd your_unzipped_location/bin` and `source setup.sh`.
- On Windows, set up with `imagetool.cmd` or the `imagetool.bat` script:
    ```cmd
    @ECHO OFF
    java -cp "your_unzipped_folder/lib" com.oracle.weblogic.imagetool.cli.CLIDriver %*
    ```

## Help Command
- After completing the [Setup](#Setup) steps, execute `imagetool help` to show the help screen.
- You can execute the JAR directly using the command `java -cp "your_unzipped_folder/lib" com.oracle.weblogic.imagetool.cli.CLIDriver help`.

## Simple Example
Let's give this a try!  
- Get the installers from Oracle e-Delivery.
- Add those installers to the Image Tool file cache.
- Assuming Docker is installed, run `imagetool create`!
```bash
unzip imagetool-0.1.zip
cd imagetool-0.1/bin
source setup.sh
imagetool cache addInstaller --type jdk --version 8u202 --path /tmp/jdk-8u202-linux-x64.tar.gz
imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /tmp/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
imagetool create --tag sample:wls
```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
