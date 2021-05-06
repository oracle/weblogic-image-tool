---
title: "Create an image without Internet access"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 2
---


In this use case, because there is no Internet access, you will need to download all the installers and
patches, plus set up the cache.  Also, you must provide a base operating system image that has the following packages installed:

* `gzip`
* `tar`
* `unzip`


### Steps

1. Download these Java and WebLogic installers from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com)
and save them in a directory of your choice, for example, `/home/acmeuser/wls-installers`:

     `fmw_12.2.1.3.0_wls_Disk1_1of1.zip`\
     `jdk-8u202-linux-x64.tar.gz`


2. Use the [Cache Tool]({{< relref "/userguide/tools/cache.md" >}}) to add the installers:

    ```bash
    imagetool cache addInstaller --type jdk --version 8u202 --path /home/acmeuser/wls-installers/jdk-8u202-linux-x64.tar.gz
    ```

    ```bash
    imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    ```

3. For each WebLogic patch, download it from [Oracle Support](https://support.oracle.com/) and set up the cache.

    For example, if you download patch number `27342434` for WebLogic Server version 12.2.1.3.0:

   ```bash
   imagetool cache addPatch --patchId 27342434_12.2.1.3.0 --path /home/acmeuser/cache/p27342434_122130_Generic.zip
   ```

   **Note**: Refer to the [Cache]({{< relref "/userguide/tools/cache.md" >}}) commands for the format of ```patchId```.

4. Then, run the command to create the image:

   ```bash
   imagetool create --fromImage myosimg:latest --tag wls:12.2.1.3.0 --patches 27342434 --version 12.2.1.3.0
   ```

5. Occasionally, a WebLogic Server patch will require patching the OPatch binaries before applying them.  We recommend downloading the latest OPatch patch and setting up the cache.  

    For example, if the latest OPatch patch is `28186730`, `13.9.4.0.0`.  After downloading it from Oracle Support, you can use this command to set up the cache:

  ```bash
  imagetool cache addPatch --patchId 28186730_13.9.4.0.0 --path /home/acmeuser/cache/p28186730_139400_Generic.zip
  ```
