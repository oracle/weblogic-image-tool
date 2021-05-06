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

For detailed information, see [Image Tool]({{< relref "/userguide/tools/_index.md" >}}).

***
### Current production release

WebLogic Image Tool version and release information can be found [here](https://github.com/oracle/weblogic-image-tool/releases).

***
### Recent changes and known issues

See the [Release Notes]({{< relref "/release-notes.md" >}}) for known issues and workarounds.


### About this documentation

* Use the [Quick Start]({{< relref "/quickstart/quickstart.md" >}}) guide to create a Linux based WebLogic Docker image.
* The [User Guide]({{< relref "/userguide/" >}}) contains installation prerequisites, setup information, command options, and detailed usage information.
* The [Samples]({{< relref "/samples/" >}}) provide informative use case scenarios.
* The [Developer Guide]({{< relref "/developer/" >}}) provides details for people who want to understand how WIT is built.


### Related projects

* [WebLogic Kubernetes Operator](https://oracle.github.io/weblogic-kubernetes-operator/)
* [WebLogic Deploy Tooling](https://oracle.github.io/weblogic-deploy-tooling/)
* [WebLogic Monitoring Exporter](https://github.com/oracle/weblogic-monitoring-exporter)
* [WebLogic Logging Exporter](https://github.com/oracle/weblogic-logging-exporter)
* [WebLogic Remote Console](https://github.com/oracle/weblogic-remote-console)

### Contributing
Those who wish to contribute to the WebLogic Image Tool code will find useful information [here](https://github.com/oracle/weblogic-image-tool/blob/main/CONTRIBUTING.md).
