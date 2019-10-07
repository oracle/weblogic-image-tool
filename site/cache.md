# Cache

The Image Tool maintains a local file cache store. This store is used to look up where the Java, WebLogic Server installers, and WebLogic Server patches reside in the local file system.

By default, the cache store is located in the user's ```$HOME/cache``` directory.  Under this directory, the lookup information is stored in the ```.metadata``` file.  All automatically downloaded patches also reside in this directory.  

You can change the default cache store location by setting the environment variable `WLSIMG_CACHEDIR`:

```bash
export WLSIMG_CACHEDIR="/path/to/cachedir"
```

You use the `cache` command to manipulate the local file cache. There are several subcommands for the cache feature.

```
Usage: imagetool cache [OPTIONS]
List and set cache options
```

| Option | Description |
| --- | --- |
|`listItems`| List cache contents. |
|`addInstaller` | Add cache entry for `wls`, `fmw`, `jdk`, or `wdt` installer. |
| `addPatch` | Add cache entry for `wls` or `fmw` patch, or `psu`.  |
| `addEntry` | Add a cache entry. Use with caution. |  
| `help` | Display help information for the specified command.|


## Usage scenarios

- `listItems`: Display the contents of the cache. Displays key value pairs of the installers and patches.
    ```
    imagetool cache listItems

    Cache contents
    jdk_8u202=/home/acmeuser/Downloads/cache/server-jre-8u202-linux-x64.tar.gz
    wls_12.2.1.3.0=/home/acmeuser/Downloads/cache/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    28186730_opatch=/home/acmeuser/cache/p28186730_139400_Generic.zip
    29135930_12.2.1.3.190416=/home/acmeuser/cache/p29135930_12213190416_Generic.zip
    29135930_12.2.1.3.0=/home/acmeuser/cache/p29135930_122130_Generic.zip
    cache.dir=/home/acemeuser/cache
    ```

- `addInstaller`: Add an installer to the cache, for example, JDK.
    ```
    imagetool cache addInstaller --type jdk --version 8u202 --path /path/to/local/jdk.tar.gz
    ```

- `addPatch`: Add a patch to the cache. This command verifies if the path points to a valid patch by querying the Oracle Support portal.
    ```
    imagetool cache addPatch --type wls --patchId 12345678_12.2.1.3.0 --path /path/to/patch.zip
    ```
    **Note**:  When adding a patch to the cache store, the `patchId` should be in the following format:  `99999999_9.9.9.9.99999`  The first 8 digits is the patch ID, followed by an underscore, and then the release number.  This is needed if you want to distinguish a patch that has different patch versions.  

    For example, patch `29135930` has several different versions in Oracle Support, one for each release in which the bug is fixed.

| Patch Name | Release |
| ---------|---------|
| `29135930` | `12.2.1.3.190416`|
| `29135930` | `12.2.1.3.0` |
| `29135930` | `12.2.1.3.18106` |

If you downloaded the release version ```12.2.1.3.190416``` of the patch, then you should use the argument ```--patchId 29135930_12.2.1.3.190416```.

- `addEntry`: Consider this an expert mode where you can add key value pairs to the cache without any validation.
    ```
    imagetool cache addEntry --key xyz_123 --value /path/to/file
    ```

- `deleteEntry`: Delete an entry from the cache for a given key. **Note**: This command does not delete files from the disk.
    ```
    imagetool cache deleteEntry --key xyz_123
    ```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates.  All rights reserved.
