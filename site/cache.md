# Cache

Use the `cache` command to add or edit the required metadata for the tool. Cache helps the tool to identify where the
required installers (WLS, FMW, JDK) are located and where to download the patches. There are several subcommands:

```
Usage: imagetool cache [COMMAND]
List and set cache options

Commands:

  listItems     List cache contents
  addInstaller  Add cache entry for wls, fmw, jdk or wdt installer
  addPatch      Add cache entry for wls|fmw patch or psu
  getCacheDir   Prints the cache directory path
  setCacheDir   Sets the cache directory where to download required artifacts
  addEntry      Command to add a cache entry. Use caution
  deleteEntry   Command to delete a cache entry
  help          Displays help information about the specified command
```

## Usage scenarios

- `listItems`: Display the contents of the cache. Displays key value pairs of the installers and patches.
    ```
    imagetool cache listItems

    Cache contents
    jdk_8u202=/Users/xyz/Downloads/cache/server-jre-8u202-linux-x64.tar.gz
    12345678_600000000012345=/Users/xyz/Downloads/cache/p12345678_122130_Generic.zip
    87654321_600000000654321=/Users/xyz/Downloads/cache/p87654321_139400_Generic.zip
    wls_12.2.1.3.0=/Users/xyz/Downloads/cache/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    cache.dir=/Users/xyz/Downloads/cache
    ```

- `addInstaller`: Add an installer to the cache, for example, JDK.
    ```
    imagetool cache addInstaller --type jdk --version 8u202 --path /path/to/local/jdk.tar.gz
    ```

- `addPatch`: Add a patch to the cache. This command verifies if the path points to a valid patch by querying the Oracle support portal.
    ```
    imagetool cache addPatch --type wls --version 12.2.1.3.0 --user abc@xyz.com --passwordEnv MYVAR --patchId p12345678 --path /path/to/patch.zip
    ```

- `getCacheDir` and `setCacheDir`: Get or set the cache directory. Used to display or set the directory where patches will be downloaded.
    ```
    imagetool cache getCacheDir
    imagetool cache setCacheDir /path/to/dir
    ```

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

