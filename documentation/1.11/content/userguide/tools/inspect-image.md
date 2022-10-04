---
title: "Inspect Image"
date: 2021-07-06
draft: false
weight: 10
description: "The inspect command reports on the contents of a container image."
---

The `inspect` command reports on the contents of a container image providing version and location information for Java
and WebLogic installations.

```
Usage: imagetool inspect [OPTIONS]
```

| Parameter | Definition | Default |
| --- | --- | --- |
| `--image`, `-i` | (Required) The image ID or image name to be inspected.  |   |
| `--builder`, `-b` | Executable to inspect Docker images. Use the full path of the executable if not on your path. | Defaults to `docker`, or, when set, to the value in environment variable `WLSIMG_BUILDER`. |
| `--format` | The output format. Supported values: `JSON` | `JSON`  |
| `--patches` | Include OPatch information in the output, including a list of WebLogic patches that are applied.  |   |

#### Use an argument file

You can save all arguments passed for the Image Tool in a file, then use the file as a parameter.

For example, create a file called `build_args`:

```bash
inspect
--image wls:12.2.1.4.0
--patches
```

Use the argument file on the command line, as follows:

```bash
$ imagetool @/path/to/build_args
```

### Usage scenarios

- Inspect an image created with the `create` command where the latest PSU was applied.
    ```bash
    $ imagetool.sh inspect --image example:12214 --patches
    ```
  The output will be in JSON format and will be similar to:
    ```json
    {
      "oraclePatches" : [
        {
          "patch" : "xxxxxxx",
          "description" : "WLS PATCH SET UPDATE 12.2.1.4.xxxxxxx"
        }
      ],
      "javaHome" : "/u01/jdk",
      "javaVersion" : "1.8.0_xxx",
      "opatchVersion" : "13.9.4.2.5",
      "oracleHome" : "/u01/oracle",
      "oracleHomeGroup" : "oracle",
      "oracleHomeUser" : "oracle",
      "oracleInstalledProducts" : "WLS,COH,TOPLINK",
      "packageManager" : "YUM",
      "wlsVersion" : "12.2.1.4.0"
    }
    ```

- Inspect the same image without the optional `--patches` switch.
    ```bash
    $ imagetool.sh inspect --image example:12214
    ```
  The output will be in JSON format and will be similar to:
    ```json
    {
      "javaHome" : "/u01/jdk",
      "javaVersion" : "1.8.0_xxx",
      "oracleHome" : "/u01/oracle",
      "oracleHomeGroup" : "oracle",
      "oracleHomeUser" : "oracle",
      "oracleInstalledProducts" : "WLS,COH,TOPLINK",
      "packageManager" : "YUM",
      "wlsVersion" : "12.2.1.4.0"
    }
    ```
