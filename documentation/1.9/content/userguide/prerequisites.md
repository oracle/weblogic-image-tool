---
title: "Prerequisites"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 1
---

- Docker client and daemon on the build machine, with minimum Docker version 18.03.1.ce.
- OPTIONALLY: Podman 3.x may be used in place of Docker.  
- Installers for WebLogic Server and JDK from the [Oracle Software Delivery Cloud](https://edelivery.oracle.com).
- When using any of the patching options, `--patches`, `--recommendedPatches`, or `--latestPSU`, you will need to provide [Oracle Support](https://www.oracle.com/technical-resources/) credentials.
- When using the `imagetool` alias from `setup.sh` instead of the shell script (`imagetool.sh`), Bash version 4.0 or later is required for `<tab>` command completion.
