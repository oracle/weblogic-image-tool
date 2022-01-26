# WebLogic Image Tool

Oracle is finding ways for organizations using WebLogic Server to run important workloads, to move those workloads into
the cloud, and to simplify and speed up the application deployment life cycle. By adopting industry standards, such as Docker
and Kubernetes, WebLogic now runs in a cloud neutral infrastructure.  To help simplify and automate the creation of
container images for WebLogic Server, we are providing this open source WebLogic Image Tool (WIT).  

WIT let's you create a new Linux based image, with installations of a JDK and WebLogic Server, and optionally,
configure a WebLogic domain with your applications, apply WebLogic Server patches, or update an existing image.


With the Image Tool you can:

* Create a Docker image and install the requested Java and WebLogic software. And, you can create a
WebLogic domain in the image, at the same time.
* Create a new Docker image using an existing WebLogic domain from an existing image. The Docker
image can start from an existing image with a JDK and Oracle middleware installation, or can install
the JDK and Oracle Home as part of moving the domain.
* Create a new Docker image by applying WebLogic patches to an existing image. In addition,
 if one did not exist previously, you can create a WebLogic domain, update an existing domain, or deploy an application.

For detailed information, see [Image Tool](https://oracle.github.io/weblogic-image-tool/userguide/tools/).

***
### Current production release

WebLogic Image Tool version and release information can be found [here](https://github.com/oracle/weblogic-image-tool/releases).

**NOTE**: The upcoming change to Oracle Support's recommended patches list will require  
Image Tool 1.9.11 in order to use `--recommendedPatches` or `--latestPSU`.

## Documentation

Documentation for WebLogic Image Tool is [available here](https://oracle.github.io/weblogic-image-tool/).

This documentation includes information for users and for developers.

## Related projects

* [WebLogic Kubernetes Operator](https://oracle.github.io/weblogic-kubernetes-operator/)
* [WebLogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/)
* [WebLogic Kubernetes Toolkit UI](https://oracle.github.io/weblogic-toolkit-ui/)
* [WebLogic Monitoring Exporter](https://github.com/oracle/weblogic-monitoring-exporter)
* [WebLogic Logging Exporter](https://github.com/oracle/weblogic-logging-exporter)
* [WebLogic Remote Console](https://oracle.github.io/weblogic-remote-console/)

## Contributing

This project welcomes contributions from the community. Before submitting a pull
request, please [review our contribution guide](./CONTRIBUTING.md).

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security
vulnerability disclosure process.

## License

Copyright (c) 2019, 2021 Oracle and/or its affiliates.

Released under the Universal Permissive License v1.0 as shown at
<https://oss.oracle.com/licenses/upl/>.
