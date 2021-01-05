# Patch an existing image

This example shows you how to apply WebLogic Server patches to an existing image.
You can download patches using the Image Tool or by manually downloading them.

## Steps

1. Create the image, as directed in the [Quick Start](quickstart.md) guide.

2. For each WebLogic patch, download it from [Oracle Support](https://support.oracle.com/keystone/) and set up the cache.

    For example, to download patch number `27342434` for WebLogic Server version 12.2.1.3.0:

  ```bash
  imagetool cache addPatch --patchId 27342434_12.2.1.3.0 --path /home/acmeuser/cache/p27342434_122130_Generic.zip
  ```

  **Note**: Refer to the [Cache](cache.md) commands for the format of ```patchId```.

3. Use the [`imagetool update`](update-image.md) command to update the image:

  ```bash
  imagetool update --fromImage wls:12.2.1.3.0 --tag wls:12.2.1.3.4 --patches 27342434
  ```

## Copyright
Copyright (c) 2019, 2021, Oracle and/or its affiliates.
