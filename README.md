# Oracle WebLogic Image Builder Tool

Containerization is taking over the software world. In this modern age of software development, Docker and Kubernetes
have become the gold standard. To help customers stay at the forefront of this, Oracle has released the
[WebLogic Server Kubernetes Operator](https://github.com/oracle/weblogic-kubernetes-operator). The operator helps
customers move their workloads to the cloud via the Kubernetes (cloud neutral) route. Customers get to decide how
to create the Docker images and apply patches to these images deployed by the Kubernetes operator. To help them
with this effort, we have created the [WebLogic Image Builder Tool](https://github.com/oracle/weblogic-imagebuilder-tool).

## Table of Contents

- [Features](#features-of-the-imagebuilder-tool)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
  - [Create Image](site/create-image.md)
  - [Update Image](site/update-image.md)
  - [Cache](site/cache.md)

## Features of the Oracle WebLogic Image Builder Tool

The Image Builder Tool can create a WebLogic Docker image from any base image (for example, Oracle Linux, Ubuntu), install a given version of
WebLogic (for example, 12.2.1.3.0), install a given JDK (for example, 8u202), apply selected patches, and create a domain with a given
version of [WDT](https://github.com/oracle/weblogic-deploy-tooling). The tool can also update a Docker image either
created using this tool or a Docker image built by the user (you should have an `ORACLE_HOME` `env` variable defined by
applying selected patches).

## Prerequisites

- Active Internet connection. The tool needs to communicate with Oracle support system.
- Oracle support credentials. These are used to validate and download patches.
- WebLogic and JDK installers from OTN / e-delivery. These should be available on local disk.
- Docker client and daemon on the build machine. Experimental features should be turned on to support the `docker build --squash` option. Refer to [Docker squash](https://docs.docker.com/engine/reference/commandline/build/#squash-an-images-layers---squash-experimental)

## Setup

- Build the project (`mvn clean package`) to generate artifacts `imagebuilder-0.1-SNAPSHOT.jar` and `imagebuilder_completion.sh`.
- The JAR is an uber-JAR containing all the required packages to run the tool.
- If running an OS with bash, then use the commands below to set up the tool with the tab complete feature:
   ```bash
   #!/usr/bin/env bash
   unalias imagebuilder 2> /dev/null
   alias imagebuilder='java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver'
   source ./imagebuilder_completion.sh
   ```
- On Windows, set up with `imagebuilder.cmd` or the `imagebuilder.bat` script:
    ```cmd
    @ECHO OFF
    java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver %*
    ```
- Then, execute `imagebuilder help` to get the help screen.
- You can execute the JAR directly using the command `java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver help`.
- After you are familiar with the commands, you will be able to create and update WebLogic Docker images.
