# Oracle WebLogic Image Tool

Oracle is finding ways for organizations using WebLogic Server to run important workloads, to move those workloads into
the cloud, and to simplify and speed up the application deployment life cycle. By adopting industry standards, such as Docker
and Kubernetes, WebLogic now runs in a cloud neutral infrastructure.  To help simplify and automate the creation of
Docker images for WebLogic Server, we are providing this open-source
Oracle WebLogic Image Tool.  This tool let's you create a new Linux based image, with installations of a JDK and WebLogic Server,
and optionally, configure a WebLogic domain with your applications, apply WebLogic Server patches, or update an existing
image.

## Features

The Image Tool provides three functions within the main script:
  - [Create Image](site/create-image.md) - The `create` command creates a new Docker image and installs the requested
  Java and WebLogic software.  Additionally, you can create a WebLogic domain in the image at the same time.
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

Use the [Quick Start](site/quickstart.md) guide to create a Linux-based WebLogic Docker image.

## Samples

* [Create an image with full Internet access](site/create-image-with-internet.md)
* [Create an image with no Internet access](site/create-image-no-internet.md)
* [Patch an existing image](site/patching-image.md)
* [Create an image with a WebLogic domain using the WebLogic Deploy Tool](site/create-image-wdt.md)

## Additional tasks

* [Cleanup](site/cleanup.md)
* [Logging](site/logging.md)


## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
