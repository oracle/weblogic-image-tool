---
title: "Cross-Platform and Multi-Platforms Build"
date: 2025-01-11
draft: false
weight: 1
description: "Experimental Support for building cross-platform and multi-platforms images."
---

This version supports experimental building multi-platform images using `Docker` or `Podman` in a single `create` command.  
We have encountered any cross-platform or multi-platforms build may hang during build.  You can always create separate images and manually create manifest to build a multi-platform images. 

## Prerequisite:

When building cross or multi-platform images, the `docker` or `podman` command depends on QEMU emulation setup in the build environment. In 
a Linux environment, you can add QEMU support depending on your distribution and releases.  You will also need to setup
the `bin-fmt` support in the container environment.  Note: If you reboot, reset or purge the build system, you will need
to setup again before building cross platform images.  For `podman` the equivalent command is `sudo podman` instead of 
`docker`. 

```bash
$ docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
```

In ARM64, you can use

```bash
$ docker run --privileged --rm tonistiigi/binfmt --install all
```

This will install all the emulated platform binaries.  You need to verify the binaries are actually installed in

```bash
$ ls -l /proc/sys/fs/binfmt_misc/
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-aarch64
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-arm
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-mips64
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-mips64el
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-ppc64le
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-riscv64
-rw-r--r--. 1 root root 0 Jan  1 19:55 qemu-s390x
--w-------. 1 root root 0 Jan  1 19:54 register
-rw-r--r--. 1 root root 0 Jan  1 19:54 status

```

You can verify if you can run a pod using the emulator for a different platform. For example, if you are in a `amd64`
environment and want to run `arm64` images, then you can:

```bash
$ docker run -it --platform linux/arm64 --rm ghcr.io/oracle/oraclelinux:8-slim sh 
```
In the terminal session, run `microdnf update`

If the commands are successful, then the emulation is working.  If it fails, then you can 
check `/var/log/messages` or `/var/log/audit/audit.log`  for errors.  Most of the issues are related to `SELinux` settings,
a quick work around is set to `permissive` using:

```bash
$ sudo setenforce 0
```

You may need administrator and security expert to help to solve any SELinux related issue in your environment.

If you want to unregister an particular emulator, you can

```bash
$ sudo echo '-1' > /proc/sys/fs/binfmt_misc/qemu-aarch64
```

For unregistering all emulators

```bash
$ for f in /proc/sys/fs/binfmt_misc/qemu*; do
    sudo echo '-1' > "$f"
  done
```

## Creating image using a single command:

1. Make sure the installers are added to the cache, for example:

```bash
$ imagetool.sh cache addInstaller --type wls --architecture AMD64 --version 14.1.1.0.0 --path /path/to/wls12214-amd-install.zip
$ imagetool.sh cache addInstaller --type wls --architecture ARM64 --version 14.1.1.0.0 --path /path/to/wls12214-arm-install.zip
$ imagetool.sh cache addInstaller --type jdk --architecture AMD64 --version 11u22 --path /path/to/jdk-11u22-amd.tar.gz
$ imagetool.sh cache addInstaller --type jdk --architecture ARM64 --version 11u22 --path /path/to/jdk-11u22-arm.tar.gz
```

2. Create the image:

```bash
$ imagetool.sh create --tag myrepo/wls14110:20250111 --platform linux/arm64,linux/amd64 --version 14.1.1.0.0 --jdkVersion 11u22 --push
```

This command will build the multi-platform image containing both `linux/arm64` and `linux/amd64` in the manifest and push to the repository `myrepo`.

## Creating image using multiple commands:

In case the emulation failed when building cross platform image,  you can always do it manually with multiple commands.

1. Build the `linux/amd64` image in a `amd64` machine and push it to the repository.

```bash
$ imagetool.sh create --tag myrepo/wls14110:20250111-amd64 --platform linux/amd64 --version 14.1.1.0.0 --jdkVersion 11u22
$ docker push myrepo/wls14110:20250111-amd64
```

2. Build the `linux/arm64` image in a `arm64` machine and push it to the repository.

```bash
$ imagetool.sh create --tag myrepo/wls14110:20250111-arm64 --platform linux/arm64 --version 14.1.1.0.0 --jdkVersion 11u22
$ docker push myrepo/wls14110:20250111-arm64
```

3. Create the manifest.

```bash
$ docker manifest create myrepo/wls14110:20250111
```

4. Add the images to the manifest.

```bash
$ docker manifest add myrepo/wls14110:20250111 myrepo/wls14110:20250111-amd64
$ docker manifest add myrepo/wls14110:20250111 myrepo/wls14110:20250111-arm64
```

5. Push the manifest to the repository.

```bash
$ docker manifest push myrepo/wls14110:20250111 myrepo/wls14110:20250111
```


## Known Problems.

1. During building cross platform image, for example if you are on a `AMD644` platform building a `ARM64` platform, you will
see it stuck in `Copying Files`

```
#31 14.93 Setting ORACLE_HOME...
#31 20.06 Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
#31 20.07 Reading response file..
#31 20.17 Skipping Software Updates
#31 20.18 Validations are disabled for this session.
#31 20.18 Verifying data
#31 30.47 Copying Files
#31 52.05 Percent Complete : 10
#31 56.19 Percent Complete : 20

```

If you run the command `ps axww | grep qemu` on the build machine, you will find duplicate java processes similar to this

```
1807143 ?        Sl     2:01 /usr/bin/qemu-aarch64-static  /u01/jdk/bin/java -cp /tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/engine-nextgen.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/message.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/oneclick.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.dms/dms.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl2.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl-log4j.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.jewt/jewt4.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.jewt/olaf2.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/jlib/wizardCommonResources.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.share/share.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/oracle_ice.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/ohj.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/help-share.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/modules/installer-launch.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/modules/xml.jar -mx512m -Doracle.installer.appendjre=true -Djava.io.tmpdir=/tmp -Doracle.installer.jre_loc=/u01/jdk -Doracle.installer.custom_inventory=/u01/oracle/oraInventory -Doracle.cie.logging.useodl=true -Doracle.installer.startup_location=/tmp/orcl4209283845595543233.tmp/Disk1/install/linux64 -Doracle.installer.nlsEnabled=true -Doracle.installer.bootstrap=true -Doracle.installer.paramFile=/tmp/orcl4209283845595543233.tmp/Disk1/install/linux64/oraparam.ini -Doracle.installer.oui_loc=/tmp/OraInstall2025-02-07_01-38-49PM/oui -Doracle.installer.launch_loc=/ -Doracle.installer.unixVersion=6.8.0-39-generic -Doracle.installer.scratchPath=/tmp/OraInstall2025-02-07_01-38-49PM -Doracle.installer.extjre=true -Doracle.installer.timestamp=2025-02-07_01-38-49PM -Doracle.installer.operation=install -DisNextGen=true -Doracle.installer.prereqConfigLoc=/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/wls/prereq -Doracle.installer.library_loc=/tmp/OraInstall2025-02-07_01-38-49PM/oui/lib/linux64 -Doracle.installer.logPath=/tmp/OraInstall2025-02-07_01-38-49PM oracle.sysman.oio.oioc.OiocOneClickInstaller -scratchPath /tmp/OraInstall2025-02-07_01-38-49PM -sourceType network -timestamp 2025-02-07_01-38-49PM -paramFile /tmp/orcl4209283845595543233.tmp/Disk1/install/linux64/oraparam.ini -silent ORACLE_HOME=/u01/oracle -responseFile /tmp/imagetool/wls/linux/amd64/wls.rsp -invPtrLoc /u01/oracle/oraInst.loc -ignoreSysPrereqs -force -novalidation -nocleanUpOnExit
1809542 ?        Sl     0:00 /usr/bin/qemu-aarch64-static  /u01/jdk/bin/java -cp /tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/engine-nextgen.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/message.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/common/framework/jlib/oneclick.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.dms/dms.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl2.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.odl/ojdl-log4j.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.jewt/jewt4.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.jewt/olaf2.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/jlib/wizardCommonResources.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.bali.share/share.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/oracle_ice.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/ohj.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oracle_common/modules/oracle.help/help-share.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/modules/installer-launch.jar:/tmp/OraInstall2025-02-07_01-38-49PM/oui/modules/xml.jar -mx512m -Doracle.installer.appendjre=true -Djava.io.tmpdir=/tmp -Doracle.installer.jre_loc=/u01/jdk -Doracle.installer.custom_inventory=/u01/oracle/oraInventory -Doracle.cie.logging.useodl=true -Doracle.installer.startup_location=/tmp/orcl4209283845595543233.tmp/Disk1/install/linux64 -Doracle.installer.nlsEnabled=true -Doracle.installer.bootstrap=true -Doracle.installer.paramFile=/tmp/orcl4209283845595543233.tmp/Disk1/install/linux64/oraparam.ini -Doracle.installer.oui_loc=/tmp/OraInstall2025-02-07_01-38-49PM/oui -Doracle.installer.launch_loc=/ -Doracle.installer.unixVersion=6.8.0-39-generic -Doracle.installer.scratchPath=/tmp/OraInstall2025-02-07_01-38-49PM -Doracle.installer.extjre=true -Doracle.installer.timestamp=2025-02-07_01-38-49PM -Doracle.installer.operation=install -DisNextGen=true -Doracle.installer.prereqConfigLoc=/tmp/OraInstall2025-02-07_01-38-49PM/oui/mw/wls/prereq -Doracle.installer.library_loc=/tmp/OraInstall2025-02-07_01-38-49PM/oui/lib/linux64 -Doracle.installer.logPath=/tmp/OraInstall2025-02-07_01-38-49PM oracle.sysman.oio.oioc.OiocOneClickInstaller -scratchPath /tmp/OraInstall2025-02-07_01-38-49PM -sourceType network -timestamp 2025-02-07_01-38-49PM -paramFile /tmp/orcl4209283845595543233.tmp/Disk1/install/linux64/oraparam.ini -silent ORACLE_HOME=/u01/oracle -responseFile /tmp/imagetool/wls/linux/amd64/wls.rsp -invPtrLoc /u01/oracle/oraInst.loc -ignoreSysPrereqs -force -novalidation -nocleanUpOnExit
```

It appears to be a `QEMU` process not handling sub process correctly, you can terminate the last process by `sudo kill -9 <pid>`. For example
`sudo kill -9 1809542` then the build will continue and complete.  The best way to solve this is use manual build in each respective platform as mentioned in last section.


2. You may encounter `SELinux` security issue, usually it appears some build commands failed.  You can check for errors in `/var/log/messages` and search for
for `QEMU`.  In this case, you need to work with your administrator either relax the security policy or setup customized policy. This is 
environment specific.
