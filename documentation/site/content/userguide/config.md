---
title: "Configuration"
date: 2024-04-16T08:00:00-05:00
draft: false
weight: 5
description: "Learn about advanced configuration of the Image Tool."
---

The default configuration is typically adequate for common use. But, the following environment variables are provided for
non-typical use cases when the default values are insufficient.  

### Environment variables

- `WLSIMG_BLDDIR` - During the build process, Image Tool creates a Docker context directory where it will create a Dockerfile and copy necessary files for the container image build. Setting this variable to another directory overrides the default of the user's home directory as the parent folder of the Docker context directory.
- `WLSIMG_BUILDER` - As an alternative to the command-line argument `--builder`, this variable can be used to override the tool to process the Dockerfile (such as`docker` or `podman`). The provided value should be the full path to the executable. For example, `WLSIMG_BUILDER="/usr/bin/docker"`.
- `WLSIMG_CACHEDIR` - When Image Tool downloads patches, those patches are saved in the cache directory. Setting this variable to another directory overrides the default of the `cache` folder in the user's home directory.
- `WLSIMG_OS_PACKAGES` - There are several packages and libraries that are required by the WebLogic Kubernetes Toolkit. The default packages included at build time are `gzip tar unzip libaio libnsl jq findutils diffutils`. The names for those libraries can be different depending on your preferred Linux distribution or OS version. The value that you provide in this environment variable will be used in place of the default package list.


