---
title: "Create an image with a WLS domain using WDT"
date: 2019-02-23T17:19:24-05:00
draft: false
weight: 4
---

The Image Tool lets you create a customized WebLogic domain in the image using the [WebLogic Deploy Tool](https://oracle.github.io/weblogic-deploy-tooling/).

### Steps

1. Create the image, as directed in the [Quick Start]({{< relref "/quickstart/quickstart.md" >}}) guide.

2. Download the [WebLogic Deploy Tool](https://github.com/oracle/weblogic-deploy-tooling/releases), and then add it to the cache store:

```bash
$ imagetool cache addInstaller --type wdt --version 0.22 --path /home/acmeuser/cache/weblogic-deploy.zip
```

3. Provide the command-line options for the WebLogic Deploy Tool:

```bash
$ imagetool create --fromImage myosimg:latest --tag wls:12.2.1.3.0  --version 12.2.1.3.0  --wdtVersion 1.4.0 --wdtArchive /home/acmeuser/wdt/domain1.zip --wdtDomainHome /u01/domains/simple_domain
```

The parameter mappings between the Image Tool and the WebLogic Deploy Tool are:

| Image Tool         | WebLogic Deploy Tool | Default  |
|--------------------|-------------------------|--------|
| `--wdtArchive`       | `-archive_file`           |   |
| `--wdtModel`         | `-model_file`             |   |
| `--wdtVariables`     | `-variable_file`          |   |
| `--wdtRunRCU`        | `-run_rcu`                | `false`  |
| `--wdtDomainHome`    | `-domain_home`            |   |
| `--wdtDomainType`     | `-domain_type`           |  `WLS` |


 If you do not specify ```--wdtDomainHome```, the domain will be created under ```/u01/domains/base_domain```.

**Note**: If you are creating a JRF domain and want WebLogic Deploy Tool to create the RCU schemas for you, you can
specify the connection information in the model. See [Specifying RCU
information in the model](https://oracle.github.io/weblogic-deploy-tooling/rcuinfo/).
