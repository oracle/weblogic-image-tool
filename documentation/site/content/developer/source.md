---
title: "Build from source"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 1
description: "How to build the Image Tool from source."
---

The Image Tool installer is available on the [Releases](https://github.com/oracle/weblogic-image-tool/releases) page.
If you want to build the installer from source instead, use the steps on this page.

## Prerequisites

Before building the project, install the following tools:

- JDK 11 or later
- Maven 3.6.3 or later
- Git

If you want to run integration tests, you also need:

- Docker or Podman
- Oracle Support credentials, provided through the `ORACLE_SUPPORT_USERNAME` and `ORACLE_SUPPORT_PASSWORD`
  environment variables
- A local staging directory, provided through the `STAGING_DIR` environment variable, that contains the required
  WebLogic Server installers, JDK installers, and pre-downloaded patches

## Build the installer ZIP

1. Clone the repository:

   ```shell
   git clone https://github.com/oracle/weblogic-image-tool.git
   ```

1. Change to the project root:

   ```shell
   cd weblogic-image-tool
   ```

1. Build the installer ZIP:

   ```shell
   mvn clean package
   ```

The build creates the installer ZIP file at `installer/target/imagetool.zip`.

## Maven phases

Use the standard Maven lifecycle phases shown in the following list:

- `validate` - Validate the project and build prerequisites.
- `compile` - Compile the source code.
- `test` - Run the unit tests.
- `package` - Create the installer ZIP file, `imagetool.zip`.
- `verify` - Run the integration tests.
- `clean` - Remove files created during the build.

Maven runs phases in order. For example, `mvn verify` runs `validate`, `compile`, `test`, and `package` before it
runs `verify`.

Because `package` runs before `verify`, you do not need to run integration tests to create the installer ZIP.

## Run integration tests

If you want to validate changes in your environment, run the `verify` phase after completing the prerequisites for
integration tests.

### Examples

Run a test group (`cache`, `gate`, or `nightly`):

```shell
mvn verify -Dtest.groups=cache
```

Run a single integration test:

```shell
mvn verify -Dtest.groups=gate,nightly -Dit.test=ITImagetool#createWlsImg
```

### Test groups

- `cache` - Build and manipulate the Image Tool cache.
- `gate` - Run the basic integration test suite used to validate merge requests, including several image builds
  (about 20 minutes).
- `nightly` - Run the full integration test suite, including various WLS and JRF image builds (about 2 hours).

Run the `cache` group before an integration test that builds an image, such as `createWlsImg`, so that the WLS and JDK
installers are already present in the cache.
