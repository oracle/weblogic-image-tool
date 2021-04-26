# Oracle WebLogic Image Tool

**NOTE: The upcoming change to Oracle Support's recommended patches list will require  
Image Tool 1.9.11 in order to use `--recommendedPatches` or `--latestPSU`.**

Oracle is finding ways for organizations using WebLogic Server to run important workloads, to move those workloads into
the cloud, and to simplify and speed up the application deployment life cycle. By adopting industry standards, such as Docker
and Kubernetes, WebLogic now runs in a cloud neutral infrastructure.  To help simplify and automate the creation of
Docker images for WebLogic Server, we are providing this open source
Oracle WebLogic Image Tool.  This tool let's you create a new Linux based image, with installations of a JDK and WebLogic Server,
and optionally, configure a WebLogic domain with your applications, apply WebLogic Server patches, or update an existing
image.

## Features

The Image Tool provides three functions within the main script:
  - [Create Image](site/create-image.md) - The `create` command creates a new Docker image and installs the requested
  Java and WebLogic software.  Additionally, you can create a WebLogic domain in the image at the same time.
  - [Rebase Image](site/rebase-image.md) - The `rebase` command creates a new Docker image using an existing WebLogic 
  domain from an existing image. The new Docker image can start from an existing image with a JDK and Oracle 
  middleware installation, or can install the JDK and Oracle Home as part of moving the domain.
  - [Update Image](site/update-image.md) - The `update` command creates a new Docker image by applying WebLogic patches
  to an existing image.  Additionally, you can create a WebLogic domain if one did not exist previously, update an
  an existing domain, or deploy an application.
  - [Cache](site/cache.md) - The Image Tool maintains metadata on the local file system for patches and installers.  
  The `cache` command can be used to manipulate the local metadata.

## Prerequisites

- Docker client and daemon on the build machine, with minimum Docker version 18.03.1.ce.
- Installers for WebLogic Server and JDK from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com).
- For patches, [Oracle Support](https://www.oracle.com/technical-resources/) credentials.
- Bash version 4.0 or later, to enable the `<tab>` command complete feature.

## Setup

- Build the project (`mvn clean package`) to create the ZIP installer in `./imagetool/target`.
- Unzip the release ZIP file to a desired location.
- For Linux environment, `cd your_unzipped_location/bin` and `source setup.sh`.
- For Windows environment, `cd your_unzipped_location\bin` and `.\imagetool.cmd`.
- Run `imagetool help` to show the help text.

## Quick Start

Use the [Quick Start](site/quickstart.md) guide to create a Linux based WebLogic Docker image.

## Building From Source

The Image Tool installer is available for download on the [Releases](https://github.com/oracle/weblogic-image-tool/releases) page.  
If you want to build the installer from source instead of downloading it, follow these instructions:
- Download and install JDK 8u261+
- Download and install Maven 3.6.3+
- Clone this repository to your local environment using one of the options under `Code` near the top of this page.
- From inside the top-level directory of the cloned project, `weblogic-image-tool`, using Maven, execute one or 
more of these phases:
    - `validate` - Validate the project is correct and all necessary information is available.
    - `compile`  - Compile the source code.
    - `test`     - Test the compiled source code using the JUnit5 framework.
    - `package`  - Create the installer ZIP file, `imagetool.zip`.
    - `verify`   - Run integration tests using the JUnit5 framework (Pre-requisite: Docker installed).
    - `clean`    - Restore the source by removing any items created by `package` or another phase of the build.
    
**Note:** Maven executes build phases sequentially, `validate`, `compile`, `test`, `package`, `verify`, such that 
running `verify` will run all of these phases from `validate` through `package` before executing `verify`.

Because the `package` phase comes before the `verify` phase, it is not necessary to run the integration tests to create 
the Image Tool installer.  If you are making changes and want to validate those changes in your environment, you will 
need to do some additional setup before running the `verify` phase because several of the integration tests require 
access to the Oracle Technology Network.  To run the integration tests in the 
`verify` phase, you must specify three environment variables, `ORACLE_SUPPORT_USERNAME`, `ORACLE_SUPPORT_PASSWORD`, 
and `STAGING_DIR`.  The first two, Oracle Support user name and password, are used to connect to Oracle OTN for patches.
The third, `STAGING_DIR`, should be a local folder where WebLogic Server installers, JDK installers, and pre-downloaded 
patches can be found.  The files required in the `STAGING_DIR` depend on which tests that you want to run.  

Example: Run a set of integration tests (available groups are `cache`, `gate`, and `nightly`:
```shell script
mvn verify -Dtest.groups=cache
```

Example: Run a single integration test:
```shell script
mvn verify -Dtest.groups=gate,nightly -Dit.test=ITImagetool#createWlsImg
```

Integration Test groups:
- `cache` - Tests that build and manipulate the Image Tool cache.
- `gate`  - A basic set of integration tests that are used to validate merge requests, including building several 
Docker image (~20 minutes)
- `nightly` - The full set of integration tests building various Docker images including JRF and WLS 
installations (~2 hours)

**Note:** In order to run an integration test that builds an image like `createWlsImg`, you must run the `cache` 
group first in order to populate the cache with the WLS and JDK installers.

## Samples

* [Create an image with full Internet access](site/create-image-with-internet.md)
* [Create an image with no Internet access](site/create-image-no-internet.md)
* [Patch an existing image](site/patching-image.md)
* [Create an image with a WebLogic domain using the WebLogic Deploy Tool](site/create-image-wdt.md)

## Additional tasks

* [Cleanup](site/cleanup.md)
* [Logging](site/logging.md)


## Copyright
Copyright (c) 2019, 2021, Oracle and/or its affiliates.
