+++
title = "Image Tool"
date = 2019-02-22T15:27:54-05:00
weight = 3
pre = "<b> </b>"
+++

- [Create Image]({{< relref "/userguide/tools/create-image.md" >}}): The `create` command creates a new Docker image and installs the requested Java and WebLogic software.  In addition, you can create a WebLogic domain in the image at the same time.
- [Rebase Image]({{< relref "/userguide/tools/rebase-image.md" >}}): The `rebase` command creates a new Docker image using an existing WebLogic domain from an existing image. The new Docker image can start from an existing image with a JDK and Oracle middleware installation, or can install the JDK and Oracle Home as part of moving the domain.
- [Update Image]({{< relref "/userguide/tools/update-image.md" >}}): The `update` command creates a new Docker image by applying WebLogic patches to an existing image.  In addition, you can create a WebLogic domain if one did not exist previously, update an existing domain, or deploy an application.
- [Cache]({{< relref "/userguide/tools/cache.md" >}}): The Image Tool maintains metadata on the local file system for patches and installers.  You can use the `cache` command to manipulate the local metadata.
