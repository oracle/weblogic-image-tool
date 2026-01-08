---
title: "Steps"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 3
description: "How to run the quickstart sample."
---


#### Before you begin

Make sure that you have fulfilled the [Prerequisites]({{% relref "/userguide/prerequisites.md" %}}) and [Setup]({{% relref "/userguide/setup.md" %}}) requirements.

#### Overview

The high level steps for creating an image are:

1. Download the Java and WebLogic installers from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com).
2. Add the installers to the cache store.
3. Run the ```imagetool``` command to create the image.

#### Do this

1. Download these Java and WebLogic installers from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com)
and save them in a directory of your choice, for example, `/home/acmeuser/wls-installers`:

     `fmw_12.2.1.3.0_wls_Disk1_1of1.zip`\
     `jdk-8u231-linux-x64.tar.gz`


2. Use the [Cache Tool]({{% relref "/userguide/tools/cache.md" %}}) to add the installers:

    ```bash
    $ imagetool cache addInstaller --type jdk --version 8u231 --path /home/acmeuser/wls-installers/jdk-8u231-linux-x64.tar.gz --architecture AMD64
    ```

    ```bash
    $ imagetool cache addInstaller --type wls --version 12.2.1.3.0 --path /home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip  --architecture AMD64
    ```

    You can verify the cache entries by:

    ```bash
    $ imagetool cache listInstallers
    ```

    ```bash
    JDK:
      8u231:
      - location: /home/acmeuser/wls-installers/jdk-8u231-linux-x64.tar.gz
        platform: linux/amd64
        digest: A011584A2C9378BF70C6903EF5FBF101B30B08937441DC2EC67932FB3620B2CF
        dateAdded: 2024-12-30
        version: 8u231
    WLS:
      12.2.1.3.0:
      - location: /home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
        platform: linux/amd64
        digest: CBFD847E7F8993E199C30003BCB226BA2451911747099ADC33EA5CEA2C35E0BC
        dateAdded: 2024-12-30
        version: 12.2.1.3.0
    ```

3. Run the [`imagetool create`]({{% relref "/userguide/tools/create-image.md" %}}) command:

   ```bash
   $ imagetool create --tag wls:12.2.1.3.0 --version 12.2.1.3.0 --platform linux/amd64
   ```

The final image will have the following structure:

```bash
[oracle@c3fe8ee0167d oracle]$ ls -arlt /u01/
total 20
drwxr-xr-x  7 oracle oracle 4096 Jan 28 23:40 jdk
drwxr-xr-x 11 oracle oracle 4096 Jan 28 23:40 oracle
drwxr-xr-x  5 oracle oracle 4096 Jan 28 23:40 .
drwxr-xr-x 18 root   root   4096 Jan 29 01:31 ..
```
