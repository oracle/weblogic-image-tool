---
title: "Build from source"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 1
---


The Image Tool installer is available for download on the [Releases](https://github.com/oracle/weblogic-image-tool/releases) page.
If you want to build the installer from source instead of downloading it, follow these instructions:
1. Download and install JDK 8u261+.
1. Download and install Maven 3.6.3+.
1. Clone [this](https://github.com/oracle/weblogic-image-tool) repository to your local environment.
   - Cloning options are shown under the `Code` button at the root of this project.
   - For example, `git clone https://github.com/oracle/weblogic-image-tool.git`.
1. From inside the top-level directory of the cloned project, `weblogic-image-tool`, using Maven, execute one or
more of these phases:
    - `validate` - Validate the project is correct and all necessary information is available.
    - `compile`  - Compile the source code.
    - `test`     - Test the compiled source code using the JUnit5 framework.
    - `package`  - Create the installer ZIP file, `imagetool.zip`.
    - `verify`   - Run integration tests using the JUnit5 framework (Prerequisite: Docker installed).
    - `clean`    - Restore the source by removing any items created by `package` or another phase of the build.

**Note:** Maven executes build phases sequentially, `validate`, `compile`, `test`, `package`, `verify`, such that
running `verify` will run all of these phases from `validate` through `package` before running `verify`.

Because the `package` phase comes before the `verify` phase, it is not necessary to run the integration tests to create
the Image Tool installer.  If you are making changes and want to validate those changes in your environment, you will
need to do some additional setup before running the `verify` phase because several of the integration tests require
access to the Oracle Technology Network.  To run the integration tests in the
`verify` phase, you must specify three environment variables, `ORACLE_SUPPORT_USERNAME`, `ORACLE_SUPPORT_PASSWORD`,
and `STAGING_DIR`.  The first two, Oracle Support user name and password, are used to connect to Oracle OTN for patches.
The third, `STAGING_DIR`, should be a local folder where WebLogic Server installers, JDK installers, and pre-downloaded
patches can be found.  The files required in the `STAGING_DIR` depend on which tests that you want to run.

**Example**: Run a set of integration tests (available groups are `cache`, `gate`, and `nightly`:
```shell script
$ mvn verify -Dtest.groups=cache
```

**Example**: Run a single integration test:
```shell script
$ mvn verify -Dtest.groups=gate,nightly -Dit.test=ITImagetool#createWlsImg
```

Integration Test groups:
- `cache` - Tests that build and manipulate the Image Tool cache.
- `gate`  - A basic set of integration tests that are used to validate merge requests, including building several
Docker image (~20 minutes).
- `nightly` - The full set of integration tests building various container images including JRF and WLS
installations (~2 hours).

**Note:** In order to run an integration test that builds an image like `createWlsImg`, you must run the `cache`
group first to populate the cache with the WLS and JDK installers.
