# Cache

The Image Tool maintains a local file cache for patches and installers.  The `cache` command can be used to manipulate
the local file cache. There are several subcommands of the cache feature:

```
Usage: imagetool cache [COMMAND]
List and set cache options

Commands:

  listItems     List cache contents
  addInstaller  Add cache entry for wls, fmw, jdk or wdt installer
  addPatch      Add cache entry for wls|fmw patch or psu
  addEntry      Command to add a cache entry. Use caution
  deleteEntry   Command to delete a cache entry
  help          Displays help information about the specified command
```

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

- `addPatch`: Add a patch to the cache. This command verifies if the path points to a valid patch by querying the Oracle support portal.
    ```
    imagetool cache addPatch --type wls --version 12.2.1.3.0  --patchId 12345678 --path /path/to/patch.zip
    ```
Note:  When adding a patch to the cache store. The patchId should be in the following format:  99999999_9.9.9.9.99999  The first 8 digits is the patch id, followed by an underscore and then release number.  This is needed if you want to distinguish a patch that has different versions of the patch.  

For example, patch 29135930 has several different versions from Oracle support, one for each release where the bug is fixed.

| Patch Name | Release |
| ---------|---------|
| 29135930 | 12.2.1.3.190416|
| 29135930 | 12.2.1.3.0 |
| 29135930 | 12.2.1.3.18106 |

If you have downloaded the release version ```12.2.1.3.190416``` of the patch, then you should use the argument as ```--patchId 29135930_12.2.1.3.190416```


- `addEntry`: Consider this an expert mode where you can add key value pairs to the cache without any validation.
    ```
    imagetool cache addEntry --key xyz_123 --value /path/to/file
    ```

- `deleteEntry`: Delete an entry from the cache for a given key. **Note**: This command does not delete files from the disk.
    ```
    imagetool cache deleteEntry --key xyz_123
    ```

## Copyright
Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
