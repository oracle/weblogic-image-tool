---
title: "Steps"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 3
---


#### Before you begin

Make sure that you have fulfilled the [Prerequisites]({{< relref "/quickstart/prerequisites.md" >}}) and [Setup]({{< relref "/quickstart/setup.md" >}}) requirements.

#### Overview

The high level steps for creating an image are:

1. Download the Java and WebLogic installers from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com).
2. Add the installers to the cache store.
3. Run the ```imagetool``` command to create the image.

#### Do this

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

    You can verify the cache entries by:

    ```bash
    imagetool cache listItems
    ```

    ```bash
    Cache contents
    jdk_8u202=/home/acmeuser/wls-installers/jdk-8u202-linux-x64.tar.gz
    wls_12.2.1.3.0=/home/acmeuser/wls-installers/fmw_12.2.1.3.0_wls_Disk1_1of1.zip
    ```

3. Run the [`imagetool create`](create-image.md) command:

   ```bash
   imagetool create --tag wls:12.2.1.3.0
   ```

The final image will have the following structure:

```bash
[oracle@c3fe8ee0167d oracle]$ ls -arlt /u01/
total 20
drwxr-xr-x  7 oracle oracle 4096 May 28 23:40 jdk
drwxr-xr-x 11 oracle oracle 4096 May 28 23:40 oracle
drwxr-xr-x  5 oracle oracle 4096 May 28 23:40 .
drwxr-xr-x 18 root   root   4096 May 29 01:31 ..
```
