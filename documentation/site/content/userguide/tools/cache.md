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

e.g.

imagetool.sh cache listInstallers

JDK:
  8u401:
  - location: /home/acmeuser/Downloads/jdk-8u401-fcs-bin-b10-linux-aarch64-19_dec_2023.tar.gz
    platform: linux/arm64
    digest: 811af9aa1ce2eaa902c7923ea1a5b7012bddb787df0805aded5f3663b210aa47
    dateAdded: 2024-12-30
    version: 8.0.401
  - location: /home/acmeuser/Downloads/jdk-8u401-linux-x64.tar.gz
    platform: linux/amd64
    digest: 19684fccd7ff32a8400e952a643f0049449a772ef63b8037d5b917cbd137d173
    dateAdded: 2024-12-30
    version: 8.0.401
  11u22:
  - location: /home/acmeuser/Downloads/jdk-11.0.22_linux-aarch64_bin.tar.gz
    platform: linux/arm64
    digest: 97ee39f2a39ab9612a06b0d56882571f701cad082b7cf169b5cfdee174aec7eb
    dateAdded: 2024-12-30
    version: 11.0.22
  - location: /home/acmeuser/Downloads/jdk-11.0.22_linux-x64_bin.tar.gz
    platform: linux/amd64
    digest: f9eaf0f224fac4ff26f146089181a155d547ebcf2e033cf4cc850efa228086ba
    dateAdded: 2024-12-30
    version: 11.0.22
WLS:
  12.2.1.4.0:
  - location: /home/acmeuser/Downloads/fmw_12.2.1.4.0_wls_generic_ARM_OCI.zip
    platform: linux/arm64
    digest: 2630e4e3d6c8998da8aa97ff6ff4a4a44f95a568de8cf9de01dcd47b753ff324
    dateAdded: 2024-12-30
    version: 12.2.1.4.0
  - location: /home/acmeuser/Downloads/fmw_12.2.1.4.0_wls_lite_Disk1_1of1.zip
    platform: linux/amd64
    digest: 4b3a2264875ce4d56cf6c4c70fc2e5895db1f5cbc39eb5d4e28e46bfa65d2671
    dateAdded: 2024-12-30
    version: 12.2.1.4.0
  14.1.1.0.0:
  - location: /home/acmeuser/Downloads/fmw14110.zip
    platform: linux/arm64
    digest: 5b09f15b1d5ecb89c7f399160b9d7ee1586177cdf06372826770293b3e22132c
    dateAdded: 2024-12-30
    version: 14.1.1.0.0
  - location: /home/acmeuser/Downloads/fmw_14.1.1.0.0_wls_lite_Disk1_1of1.zip
    platform: linux/amd64
    digest: 9e9fe0264e38c34ccaef47a262474aa94bf169af0d06b202f2630eaae648ae09
    dateAdded: 2024-12-30
    version: 14.1.1.0.0
```

- `listPatches`:  List all the patches.

```bash

Usage: imagetool cache listPatches [--patchId=<patchId>] [--version=<version>]
List patches
      --patchId=<patchId>   Patch id
      --version=<version>   Patch version



$ imagetool.sh cache listPatches

36805124:
  - location: /home/acmeuser/Downloads/oraclePatches/p36805124_122140_Generic.zip
    platform: linux/amd64
    digest: null
    dateAdded: 2024-11-18
    version: 12.2.1.4.0
  - location: /home/acmeuser/Downloads/oraclePatches/p36805124_122140_Generic.zip
    platform: generic
    digest: null
    dateAdded: 2024-11-18
    version: 12.2.1.4.0.180516
28186730:
  - location: /home/acmeuser/Downloads/cache/p28186730_1394216_Generic.zip
    platform: generic
    digest: null
    dateAdded: 2024-11-18
    version: 13.9.4.2.16
  - location: /home/acmeuser/Downloads/oraclePatches/p28186730_1394217_Generic.zip
    platform: generic
    digest: null
    dateAdded: 2024-11-18
    version: 13.9.4.2.17


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