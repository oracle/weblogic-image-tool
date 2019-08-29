Example Updating the Oracle Binaries for My Domain 
==================================================
Docker images are built in immutable layers, but I needed to patch the Oracle Home for my domain.
In a typical environment, I would run OPatch against my Oracle Home and be done.  But with a Docker
image, I cannot do that because the layer with the original Oracle Home is immutable.  I could add 
another layer with the patch, but I cannot change the original Oracle Home layer.  Adding a patch in 
a new layer means that the resulting Docker image contains all the original files plus all the files that
the patch replaced.  And, I need to do this same update for all my domain images.  In looking for a
better solution, I found that I could copy my old domain from one Docker image to another.  All I had
to do was keep my base image and domain image separate.

Here is an example of what I mean. 
### Setup
Load the WebLogic Image Tool cache with pointers to the installers:
```bash
imagetool cache addInstaller --type jdk --path ./installers/jdk-8u201-linux-i586.tar.gz --version 8u201
imagetool cache addInstaller --type wls --path ./installers/fmw_12.2.1.3.0_wls_Disk1_1of1/fmw_12.2.1.3.0_wls.jar --version 12.2.1.3.0
imagetool cache addInstaller --type wdt --path ./installers/weblogic-deploy.zip --version 1.1.1
```

Using the WebLogic Image Tool, create a base image with JDK 8 and WebLogic Server 12.2.1.3:
```bash
imagetool create --jdkVersion 8u202 --version 12.2.1.3.0 --tag base:1
```

With the WebLogic Image Tool update feature, create a new image with a domain starting with the base image:
```bash
imagetool update --fromImage base:1 --wdtModel ./my_domain.yaml --wdtVersion 1.1.1 --tag mydomain:1
```

### Create a Patched Base
Using the same WebLogic Image Tool command that we used to create `base:1`, this time I will create `base:2` 
and apply the latest PSU for 12.2.1.3.0.
```bash
imagetool create --jdkVersion 8u202 --version 12.2.1.3.0 --latestPSU --tag base:2 --user {your OTN credential} --passwordENV MY_PASSWORD
```

### Rebase My Domain
I cannot change the image `mydomain:1`, but I can copy it to a new image that is based on my patched Oracle Home.
Using the provided Dockerfile and build script, I create `mydomain:2` which is based on the patched base image `base:2`. 

```bash
docker build -tag mydomain:2 --build-arg NEW_BASE=base:2 --build-arg OLD_DOMAIN=mydomain:1 --build-arg DOMAIN_DIR=/u01/domains/base_domain --force-rm=true --no-cache 
```

### The Dockerfile
```dockerfile
ARG NEW_BASE

FROM NEW_BASE

ARG OLD_DOMAIN
ARG DOMAIN_DIR

ENV DOMAIN_PARENT=/u01/domains \
    DOMAIN_HOME=${DOMAIN_DIR} \
    PATH=${PATH}:${DOMAIN_HOME}/bin
    
COPY --from=${OLD_DOMAIN} ${DOMAIN_DIR} ${DOMAIN_DIR}

```
