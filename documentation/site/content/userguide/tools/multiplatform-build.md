---
title: "Multi-Platform Build"
date: 2025-01-11
draft: false
weight: 1
description: "Support for building multi-platform images.."
---

This version supports building multi-platform images using Docker in a single `create` command.  Using `podman` is 
problematic and we do not recommend using it.  You can however, always create separate images and manually create manifest
to build a multi-platform images. 

Prerequisite:

When building multi-platform images, the `docker` command depends on QEMU emulation setup in the build environment. In 
a Linux environment, you can add QEMU support depending on your distribution and releases.  An alternative is to install
it using the command.

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
$ docker run -it --platform linux/arm64 --rm ghcr.io/oracle/oraclelinux-8-slim sh 
```

If it successfully launch the pod and get a terminal session, then the emulation is working.  If it fails, then you can 
check `/var/log/messages` or `/var/log/audit/audit.log`  for errors.  Most of the issues are related to SELinux settings,
a quick work around is set to `permissive` using:

```bash
$ sudo setenforce 0
```

You may need administrator and security expert to help to solve any SELinux related issue in your environment.

Creating image using a single command:

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

Creating image using multiple commands:

In case the emulation failed, such as when using `podman`,  you can always do it manually with multiple commands.

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



