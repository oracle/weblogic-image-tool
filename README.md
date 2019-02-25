# Oracle WebLogic ImageBuilder Tool

Containerization is taking over the software world. In this modern age of software development, Docker and Kubernetes
have become the gold standard. To help customers stay at the forefront of this, Oracle released the
[WebLogic Server Kubernetes Operator](https://github.com/oracle/weblogic-kubernetes-operator). The operator helps
customers move their workloads to the cloud via the kubernetes (cloud neutral) route. It was left to the customers how
to create the docker images and apply patches to these images deployed by the kubernetes operator. To help customers
with this effort, We created the [WebLogic ImageBuilder Tool](https://github.com/oracle/weblogic-imagebuilder-tool).

## Table of Contents

- [Features](#features-of-the-imagebuilder-tool)
  - [Create Image](site/create-image.md)
  - [Update Image](site/update-image.md)
  - [Cache](site/cache.md)

## Features of the Oracle WebLogic ImageBuilder Tool

The tool can create a WebLogic docker image from any base image (ex: oraclelinux, ubuntu), install a given version of
WebLogic (ex: 12.2.1.3.0), install a given jdk (ex: 8u202), apply selected patches and create a domain with a given
version of [WDT](https://github.com/oracle/weblogic-deploy-tooling). The tool can also update a docker image either
created using this tool or a docker image built by the user (should have an ORACLE_HOME env variable defined) by
applying selected patches.

## Prerequisites

- Active internet connection
  - The tool needs to communicate with Oracle support system.
- Oracle support credentials
  - Oracle support credentials are used to validate and download patches.
- WebLogic and JDK installers from OTN / e-delivery
  - WebLogic and JDK installers should be available on local disk.
- Docker client and daemon on the build machine
  - Experimental features should be turned on to support docker build --squash option. Refer to [Docker squash](https://docs.docker.com/engine/reference/commandline/build/#squash-an-images-layers---squash-experimental)

## Setup

- Build the project (`mvn clean package`) to generate artifacts imagebuilder-0.1-SNAPSHOT.jar and imagebuilder_completion.sh
- The jar is an uber-jar containing all the required packages to run the tool.
- If running an os with bash, Use the below commands to setup the tool with tab complete feature.
   ```bash
   #!/usr/bin/env bash
   unalias imagebuilder 2> /dev/null
   alias imagebuilder='java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver'
   source ./imagebuilder_completion.sh
   ```
- On Windows, setup imagebuilder.cmd or imagebuilder.bat script
    ```cmd
    @ECHO OFF
    java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver %*
    ```
- Then, execute "imagebuilder help" to get the help screen
- The jar can be executed directly using command java -cp "./imagebuilder-0.1-SNAPSHOT.jar" com.oracle.weblogicx.imagebuilder.cli.CLIDriver help
- Once familiar with the commands, you should be able to create and update WebLogic docker images