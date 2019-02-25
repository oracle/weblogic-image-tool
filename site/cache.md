# Cache

The cache command is used to add / edit the required metadata for the tool. Cache helps the tool to identify where the 
required installers (wls, fmw, jdk) are located and where to download the patches to. There are several subcommands to 
aid the user.

```
Usage: imagebuilder cache [COMMAND]
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

- listItems: Display the contents of cache. Displays key value pairs of the installers, patches.
    ```
    imagebuilder cache listItems

    Cache contents
    jdk_8u202=/Users/xyz/Downloads/cache/server-jre-8u202-linux-x64.tar.gz
    12345678_600000000012345=/Users/xyz/Downloads/cache/p12345678_122130_Generic.zip
    87654321_600000000654321=/Users/xyz/Downloads/cache/p87654321_139400_Generic.zip
    wls_12.2.1.3.0=/Users/xyz/Downloads/cache/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    cache.dir=/Users/xyz/Downloads/cache
    ```

- addInstaller: Add installer to cache, ex: jdk
    ```
    imagebuilder cache addInstaller --type jdk --version 8u202 --path /path/to/local/jdk.tar.gz
    ```

- addPatch: Add patch to cache. This command verifies if the path points to a valid patch by querying Oracle support portal
    ```
    imagebuilder cache addPatch --type wls --version 12.2.1.3.0 --user abc@xyz.com --passwordEnv MYVAR --patchId 12345678 --path /path/to/patch.zip
    ```

- getCacheDir and setCacheDir: Get/Set cache directory. Used to display or set the directory where patches will be downloaded to by the tool.
    ```
    imagebuilder cache getCacheDir
    imagebuilder cache setCacheDir /path/to/dir
    ```
    
- addEntry: Consider this as an expert mode where you can add key value pairs to cache without any validation.
    ```
    imagebuilder cache addEntry --key xyz_123 --value /path/to/file
    ```

- deleteEntry: Delete a entry from cache for a given key. _Note: This command does not delete files from disk._ 
    ```
    imagebuilder cache deleteEntry --key xyz_123
    ```