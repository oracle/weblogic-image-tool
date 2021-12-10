---
title: "Setup"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 2
---

- Download the WIT release ZIP file to a desired location.
  - The latest ZIP can be found on the project [releases](https://github.com/oracle/weblogic-image-tool/releases) page.
  - Alternatively, you can download the ZIP with cUrl.
    ```shell
    curl -m 120 -fL https://github.com/oracle/weblogic-image-tool/releases/latest/download/imagetool.zip -o ./imagetool.zip
    ```
- Unzip the downloaded ZIP into a directory of your choice.  
  - All of the contents of the ZIP will be extracted into a single subdirectory named `imagetool`.
- OPTIONALLY: You may build the project (`mvn clean package`) to create the ZIP installer in `./imagetool/target` (see [Build From Source]({{< relref "/developer/source.md" >}})).
- Set the JAVA_HOME environment variable to the location of the Java install (see [Prerequisites]({{< relref "/quickstart/prerequisites.md" >}})).   
