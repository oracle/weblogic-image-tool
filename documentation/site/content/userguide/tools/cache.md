---
title: "Cache"
date: 2024-12-23
draft: false
weight: 20
description: "The Image Tool maintains metadata on the local file system for patches and installers.  You can use the cache command to manipulate the local metadata."
---

The Image Tool maintains a local file cache store. This store is used to look up where the Java, WebLogic Server installers, and WebLogic Server patches reside in the local file system.

By default, the cache store is located in the user's ```$HOME/.imagetool``` directory.  Under this directory, the lookup information is stored in the ```.metadata``` file.  All automatically downloaded patches also reside in this directory.

In this release, there is a `settings.yaml` in the cache directory.  This file controls the default settings of image tool.  For example: 

```bash
defaultBuildPlatform: linux/arm64
installerSettings:
  JDK: {defaultVersion: 8u401}
  WDT: {defaultVersion: latest}
  WLS: {defaultVersion: 12.2.1.4.0}
patchDirectory: /home/acmeuser/.imagetool/oraclePatches
```

The installers and patches details are stored separately into two different files. `installers.yaml` and `patches.yaml`.  By default these two files are stored in the same 
directory of `settings.yaml`.

You can change the default cache store location by setting the environment variable `WLSIMG_CACHEDIR`: ?? TODO 

```bash
$ export WLSIMG_CACHEDIR="/path/to/cachedir"
```

You use the `cache` command to manipulate the local file cache. There are several subcommands for the cache feature.

```
Usage: imagetool cache [OPTIONS]
List and set cache options
```

| Option | Description |
| --- | --- |
|`listInstallers`| List cache installers. |
|`listPatches`| List cache patches. |
|`addInstaller` | Add an installer to the cache. |
| `addPatch` | Add a patch to the cache.  |
| `deletePatch` | Delete a patch from the cache.  |
| `deleteInstaller` | Delete an installer to the cache.  |
| `help` | Display help information for the specified command.|


### Usage scenarios

- `listInstallers`: Display the contents of the cache. Displays key value pairs of the installers and patches.
```bash
Usage: imagetool cache listInstallers [--commonName=<commonName>]
                                      [--type=<type>] [--version=<version>]
List installers
      --commonName=<commonName>
                            Filter installer by common name.
      --type=<type>         Filter installer type.  e.g. wls, jdk, wdt
      --version=<version>   Filter installer by version.
      --details             Show the details about the installers
e.g.

imagetool.sh cache listInstallers

JDK:
  17u11:
    linux/arm64:
      version: 17.0.11
      location: /Users/acme/Downloads/jdk-17_linux-aarch64_11bin.tar.gz
  8u401:
    linux/amd64:
      version: 8.0.401
      location: /Users/acme/Downloads/jdk-8u401-linux-x64.tar.gz
    linux/arm64:
      version: 8.0.401
      location: /Users/acme/Downloads/jdk-8u401-fcs-bin-b10-linux-aarch64-19_dec_2023.tar.gz
  8u231:
    linux/amd64:
      version: 8u231
      location: /Users/acme/Downloads/jdk-8u231-linux-x64.tar.gz
  11u22:
    linux/amd64:
      version: 11.0.22
      location: /Users/acme/Downloads/jdk-11.0.22_linux-x64_bin.tar.gz
    linux/arm64:
      version: 11.0.22
      location: /Users/acme/Downloads/jdk-11.0.22_linux-aarch64_bin.tar.gz
WLS:
  12.2.1.4.0:
    linux/amd64:
      version: 12.2.1.4.0
      location: /Users/acme/Downloads/fmw_12.2.1.4.0_wls_lite_Disk1_1of1.zip
    linux/arm64:
      version: 12.2.1.4.0
      location: /Users/acme/Downloads/fmw_12.2.1.4.0_wls_generic_ARM_OCI.zip
  14.1.1.0.0:
    linux/amd64:
      version: 14.1.1.0.0
      location: /Users/acme/Downloads/fmw_14.1.1.0.0_wls_lite_Disk1_1of1.zip
    linux/arm64:
      version: 14.1.1.0.0
      location: /Users/acme/Downloads/fmw14110.zip
  12.2.1.3.0:
    linux/amd64:
      version: 12.2.1.3.0
      location: /Volumes/home/files/witsystest/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
  14.1.2.0.0:
    linux/arm64:
      version: 14.1.2.0.0
      location: /Users/acme/Downloads/fmw_14.1.2.0.0_wls_generic-091024.jar
```

- `listPatches`:  List all the patches.

```bash

Usage: imagetool cache listPatches [--patchId=<patchId>] [--version=<version>]
List patches
      --patchId=<patchId>   Patch id
      --version=<version>   Patch version
      --details             Show details about the patches


$ imagetool.sh cache listPatches

36805124:
  linux/amd64:
     - version: 12.2.1.4.0
       location: /Users/acme/oraclePatches/p36805124_122140_Generic.zip
28186730:
  Generic:
     - version: 13.9.4.2.16
       location: /Users/acme/cache/p28186730_1394216_Generic.zip
     - version: 13.9.4.2.17
       location: /Users/acme/oraclePatches/p28186730_1394217_Generic.zip


```


- `addInstaller`: Add an installer to the cache, for example, JDK.
    ```bash
  
  Usage: imagetool cache addInstaller [--force] -a=<architecture>
                                      [-c=<commonName>] -p=<filePath> [-t=<type>]
                                      -v=<version>
  Add cache entry for wls, fmw, jdk or wdt installer
        --force               Overwrite existing entry, if it exists
    -p, --path=<filePath>     Location of the file on disk. For ex:
                                /path/to/patch-or-installer.zip
    -t, --type=<type>         Type of installer. Valid values: wlsdev, wlsslim,
                                wls, fmw, soa, osb, b2b, mft, idm, oam, ohs,
                                db19, oud, oid, wcc, wcp, wcs, jdk, wdt, odi
    -v, --version=<version>   Installer version. Ex: For WLS|FMW use 12.2.1.3.0
                                For jdk, use 8u201. The version for WLS, FMW etc.
                                will be used to obtain patches.
    -a, --architecture=<architecture>
                              Installer architecture. Valid values: arm64, amd64,
                                Generic
    -c, --commonName=<commonName>
                              (Optional) common name. Valid values:  Alphanumeric
                                values with no special characters. If not
                                specified, default to the version value.  Use
                                this if you want to use a special name for the
                                particular version of the installer.

  
  
    $ imagetool cache addInstaller --type jdk --version 8u202 --path /path/to/local/jdk.tar.gz --architecture AMD64
    ```

- `addPatch`: Add a patch to the cache.
    ```bash
  
  Usage: imagetool cache addPatch [--force] -a=<architecture> [-d=<description>]
                                  -p=<filePath> --patchId=<patchId> -v=<version>
  Add cache entry for wls|fmw patch or psu
        --force               Overwrite existing entry, if it exists
    -p, --path=<filePath>     Location of the file on disk. For ex:
                                /path/to/patch-or-installer.zip
        --patchId=<patchId>   Patch number. Ex: 28186730
    -v, --version=<version>   Patch version.
    -a, --architecture=<architecture>
                              Patch architecture. Valid values: arm64, amd64,
                                Generic
    -d, --description=<description>
                              Patch description.

  
  
    $ imagetool cache addPatch --patchId 12345678_12.2.1.3.0 --path /path/to/patch.zip --architecture AMD64
    ```
    **Note**:  When adding a patch to the cache store, the `patchId` should be in the following format:  `99999999_9.9.9.9.99999`  The first 8 digits is the patch ID, followed by an underscore, and then the release number to identify the patch between different patch versions.  

    For example, patch `29135930` has several different versions in Oracle Support, one for each release or PSU in which the bug is fixed.

| Patch Name | Release |
| ---------|---------|
| `29135930` | `12.2.1.3.190416`|
| `29135930` | `12.2.1.3.0` |
| `29135930` | `12.2.1.3.18106` |

If you downloaded the release version ```12.2.1.3.190416``` of the patch, then you should use the argument ```--patchId 29135930_12.2.1.3.190416```.


- `deleteInstaller`: Delete an installer from the cache for a given key. **Note**: This command does not delete files from the disk.
    ```bash
  
  Usage: imagetool cache deleteInstaller [--architecture=<architecture>]
                                         [--cn=<commonName>] [--type=<type>]
                                         [--version=<version>]
  Delete a installer
        --architecture=<architecture>
                              Specific architecture to delete
        --cn=<commonName>     Filter by common name. 
        --type=<type>         Filter installer type.  e.g. wls, jdk, wdt
        --version=<version>   Specific version to delete

  
    $ imagetool cache deleteInstaller --version 14.1.1.0.0 --architecture ARM64 
    ```

- `deletePatch`: Delete a patch from the cache for a given key. **Note**: This command does not delete files from the disk.
    ```bash
  Usage: imagetool cache deletePatch [--architecture=<architecture>]
                                     [--patchId=<patchId>] [--version=<version>]
  Delete a patch
        --architecture=<architecture>
                              Specific architecture to delete
        --patchId=<patchId>   Bug num
        --version=<version>   Specific version to delete

  
    $ imagetool cache deletePatch --patchId 12345678 --version 14.1.1.0.0 --architecture ARM64 
    ```

- `convert`:  Convert Imagetool version 1.x to 2.0 format.

```bash
Usage: imagetool cache convert
Convert cache settings from v1 to v2
```