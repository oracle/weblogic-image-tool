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
  - [Create Image](site/create-image.md) - The `create` command helps build a WebLogic Docker image from a given base OS
  image.
  - [Update Image](site/update-image.md) - The `update` command can be used to apply WebLogic patches to an existing
  WebLogic Docker image.
  - [Cache](site/cache.md) - The Image Tool maintains a local file cache for patches and installers.  The `cache`
  command can be used to manipulate the local file cache.

## Prerequisites

- Docker client and daemon on the build machine, with minimum Docker version Docker 18.03.1.ce.
- WebLogic Server and JDK installers from OTN / Oracle e-Delivery.
- (For patches) Oracle support credentials.
- Bash version 4.0 or higher to enable the `<tab>` command complete feature.

## Setup

- Build the project (`mvn clean package`), to create the ZIP installer in ./imagetool/target.
- Unzip the release ZIP file to a desired location.
- For Linux environment `cd your_unzipped_location/bin` and `source setup.sh`. 
- For Windows environment `cd your_unzipped_location\bin` and `.\imagetool.cmd`. 
- Run `imagetool help` to show the help screen.

## Quick Start

[Image Tool Quick Start Guide](site/quickstart.md)

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
