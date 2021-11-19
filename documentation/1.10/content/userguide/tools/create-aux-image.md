---
title: "Create Auxiliary Image"
date: 2019-02-23
draft: false
weight: 2
description: "The createAuxImage command creates a new container image with WDT models, archives, and variable files."
---


The `createAuxImage` command helps build a container image from a given base OS image. 
The required option for the command is marked. There are a number of optional parameters for the create feature.

```
Usage: imagetool createAuxImage [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
| `--builder`, `-b` | Executable to process the Dockerfile. Use the full path of the executable if not on your path. | `docker`  |
| `--buildNetwork` | Networking mode for the RUN instructions during the image build.  See `--network` for Docker `build`.  |   |
| `--chown` | `userid:groupid` for JDK/Middleware installs and patches.  | `oracle:oracle` |
| `--dryRun` | Skip Docker build execution and print the Dockerfile to stdout.  |  |
| `--fromImage` | Container image to use as a base image when creating a new image. | `busybox`  |
| `--httpProxyUrl` | Proxy for the HTTP protocol. Example: `http://myproxy:80` or `http:user:passwd@myproxy:8080`  |   |
| `--httpsProxyUrl` | Proxy for the HTTPS protocol. Example: `https://myproxy:80` or `https:user:passwd@myproxy:8080`  |   |
| `--packageManager` | Override the default package manager for the base image's operating system. Supported values: `APK`, `APTGET`, `NONE`, `YUM`, `ZYPPER`  |   |
| `--pull` | Always attempt to pull a newer version of base images during the build.  |   |
| `--tag` | (Required) Tag for the final build image. Example: `store/oracle/mydomain:1`  |   |
| `--target` | Select the target environment in which the created image will be used. Supported values: `Default` (Docker/Kubernetes), `OpenShift` | `Default`  |
| `--wdtArchive` | A WDT archive zip file or comma-separated list of files.  |   |
| `--wdtHome` | The target folder in the image for the WDT install and models.  | `/auxiliary`  |
| `--wdtModel` | A WDT model file or a comma-separated list of files.  |   |
| `--wdtModelHome` | The target location in the image to copy WDT model, variable, and archive files. | `{wdtHome}/models` |
| `--wdtVariables` | A WDT variables file or comma-separated list of files.  |   |
| `--wdtVersion` | WDT version to use.  | `latest`  |

### Additional information

#### `--target`

By default, the generated WLS domain in your image will use the best practices defined by Oracle WebLogic Server.  
The `target` option allows you to toggle the defaults so that the generated domain is easier to use in the target
environment.  For example, the `--target OpenShift` option will change the file permissions in the domain directory
so that the group permissions match the user permissions.

| Target | Default File Permissions | Default File Ownership |
| --- | --- | --- |
| `Default` | `rwxr-x---` | `oracle:oracle` |
| `OpenShift` | `rwxrwx---` | `oracle:root` |

#### Use an argument file

You can save all arguments passed for the Image Tool in a file, then use the file as a parameter.

For example, create a file called `build_args`:

```bash
createAuxImage
--tag mydomain:1
--wdtModel ./my_domain.yaml
--wdtArchive ./my_domain.zip
```

Use it on the command line, as follows:

```bash
$ imagetool @/path/to/build_args
```


### Usage scenarios

The commands below assume that all the required WDT installer has been downloaded and added to the cache.
Use the [cache]({{< relref "/userguide/tools/cache.md" >}}) command to set it up.

- Create an image named `wdt:1.10` with the latest [WebLogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/) version.
    ```bash
    $ imagetool createAuxImage --tag wdt:1.10
    ```

- Create an image named `mydomain:1` with the same options as above and add a WDT model and archive.
    ```bash
    $ imagetool create --tag mydomain:1 --wdtModel /path/to/my_domain.yaml --wdtArchive /path/to/my_domain.zip
    ```
