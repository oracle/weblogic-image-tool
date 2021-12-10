---
title: "Prerequisites"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 1
---

- A container image client on the build machine, such as Docker or Podman. 
  - For Docker a minimum version of 18.03.1.ce is required.
  - For Podman a minimum version of 3.0.1 is required.
- An installed version of Java to run ImageTool, version 8+. 
- Installers for Oracle WebLogic Server and Oracle JDK from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com) for installation into the new image.
- When using the `imagetool` alias from `setup.sh` instead of the shell script (`imagetool.sh`), Bash version 4.0 or later is required for `<tab>` command completion.
- When using any of the patching options, `--patches`, `--recommendedPatches`, or `--latestPSU`, you will need to provide [Oracle Support](https://www.oracle.com/technical-resources/) credentials.

{{% notice note %}} The WebLogic Image Tool does not support a Stack Patch Bundle (SPB; see Doc ID [2764636.1](https://support.oracle.com/rs?type=doc&id=2764636.1)), because an SPB is _not_ a patch but a mechanism for applying all PSU and recommended CPU and SPU patches to a WebLogic Server installation, similar to invoking the Image Tool `create` or `update` command with the `--recommendedPatches` option.
{{% /notice %}}
