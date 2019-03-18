#
# Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# This file is not a fully functional Dockerfile. Do not use this directly.

# START %%WDT_ARGS%% #
ARG WDT_PKG
ARG WDT_MODEL
ARG DOMAIN_TYPE
ARG DOMAIN_NAME
ARG DOMAIN_PARENT
ARG DOMAIN_HOME
ARG WDT_ARCHIVE
ARG WDT_VARIABLE
ARG ADMIN_NAME
ARG ADMIN_HOST
ARG ADMIN_PORT
ARG MANAGED_SERVER_PORT
ARG SCRIPTS_DIR
ARG WDT_HOME
# END %%WDT_ARGS%% #

# START %%WDT_ENV%% #
ENV WDT_PKG=${WDT_PKG:-weblogic-deploy.zip} \
    ADMIN_NAME=${ADMIN_NAME:-admin-server} \
    ADMIN_HOST=${ADMIN_HOST:-wlsadmin} \
    ADMIN_PORT=${ADMIN_PORT:-7001} \
    MANAGED_SERVER_NAME=${MANAGED_SERVER_NAME:-} \
    MANAGED_SERVER_PORT=${MANAGED_SERVER_PORT:-8001} \
    WDT_MODEL=${WDT_MODEL:-} \
    DOMAIN_TYPE=${DOMAIN_TYPE:-WLS} \
    DOMAIN_PARENT=${DOMAIN_PARENT:-${ORACLE_HOME}/user_projects/domains} \
    DOMAIN_NAME=${DOMAIN_NAME:-base_domain} \
    WDT_ARCHIVE=${WDT_ARCHIVE:-} \
    WDT_VARIABLE=${WDT_VARIABLE:-} \
    PROPERTIES_FILE_DIR=$ORACLE_HOME/properties \
    SCRIPT_HOME="${ORACLE_HOME}" \
    WDT_HOME=${WDT_HOME:-/u01/app/weblogic-deploy} \
    SCRIPTS_DIR=${SCRIPTS_DIR:-scripts}

# DO NOT COMBINE THESE BLOCKS. It won't work when formatting variables like DOMAIN_HOME
ENV DOMAIN_HOME=${DOMAIN_PARENT}/${DOMAIN_NAME} \
    PATH=$PATH:${JAVA_HOME}/bin:${ORACLE_HOME}/oracle_common/common/bin:${ORACLE_HOME}/wlserver/common/bin:${DOMAIN_PARENT}/${DOMAIN_NAME}:${DOMAIN_PARENT}/${DOMAIN_NAME}/bin:${ORACLE_HOME}

# END %%WDT_ENV%% #

# START %%WDT_INSTALL%% #
COPY --chown=oracle:oracle ${WDT_PKG} ${WDT_MODEL} ${WDT_ARCHIVE} ${WDT_VARIABLE} ${OTMPDIR}/
COPY --chown=oracle:oracle ${SCRIPTS_DIR}/*.sh ${SCRIPT_HOME}/

RUN unzip $OTMPDIR/$WDT_PKG -d $(dirname $WDT_HOME) \
 && chmod a+x $SCRIPT_HOME/*.sh

#WORKDIR $ORACLE_HOME
RUN mkdir -p $(dirname ${DOMAIN_HOME}) \
 && mkdir -p ${PROPERTIES_FILE_DIR} \
 && if [ -n "$WDT_MODEL" ]; then MODEL_OPT="-model_file ${OTMPDIR}/${WDT_MODEL##*/}"; fi \
 && if [ -n "$WDT_ARCHIVE" ]; then ARCHIVE_OPT="-archive_file ${OTMPDIR}/${WDT_ARCHIVE##*/}"; fi \
 && if [ -n "$WDT_VARIABLE" ]; then VARIABLE_OPT="-variable_file ${OTMPDIR}/${WDT_VARIABLE##*/}"; fi \
 && cd ${WDT_HOME}/bin \
 && ${WDT_HOME}/bin/createDomain.sh \
 -oracle_home ${ORACLE_HOME} \
 -java_home ${JAVA_HOME} \
 -domain_home ${DOMAIN_HOME} \
 -domain_type ${DOMAIN_TYPE} \
 $VARIABLE_OPT \
 $MODEL_OPT \
 $ARCHIVE_OPT \
 && chmod -R a+x ${DOMAIN_HOME}/bin/*.sh

# END %%WDT_INSTALL%% #

# START %%WDT_CLEANUP%% #
RUN rm -rf ${WDT_HOME}
# END %%WDT_CLEANUP%% #

# START %%WDT_CMD%% #
# Expose admin server, managed server port
EXPOSE $ADMIN_PORT $MANAGED_SERVER_PORT
CMD ["sh", "-c", "${SCRIPT_HOME}/startAdminServer.sh"]

# END %%WDT_CMD%% #

# START %%OPATCH_1394%% #
RUN mkdir -p $OTMPDIR/opatch
COPY p28186730_139400_Generic.zip $OTMPDIR/opatch/

RUN unzip $OTMPDIR/opatch/p28186730_139400_Generic.zip -d $OTMPDIR/opatch ; \
    /u01/jdk/bin/java -jar $OTMPDIR/opatch/6880880/opatch_generic.jar -silent -invPtrLoc $INV_LOC/$ORAINST \
    oracle_home=$ORACLE_HOME

# END %%OPATCH_1394%% #

# START %%PATCH_APPLY%% #
RUN mkdir -p $OTMPDIR/patches

COPY $PATCHDIR/* $OTMPDIR/patches/

RUN $ORACLE_HOME/OPatch/opatch napply -silent -oh $ORACLE_HOME -phBaseDir $OTMPDIR/patches

# END %%PATCH_APPLY%% #

# START %%PATCH_CLEANUP%% #
RUN $ORACLE_HOME/OPatch/opatch util cleanup -silent -oh $ORACLE_HOME
# END %%PATCH_CLEANUP%% #

# START %%PKG_INSTALL%%_YUM #
# install necessary packages
RUN yum -y update \
 && yum -y install gzip tar unzip \
 && yum clean all
# END %%PKG_INSTALL%%_YUM #

# START %%PKG_INSTALL%%_APT #
# install necessary packages
RUN apt-get -y update \
 && apt-get -y upgrade \
 && apt-get -y install tar unzip \
 && apt-get -y clean all
# END %%PKG_INSTALL%%_APT #

# START %%PKG_INSTALL%%_APK #
# install necessary packages
RUN apk update \
 && apk upgrade \
 && apk add tar unzip \
 && rm -rf /var/cache/apk/*
# END %%PKG_INSTALL%%_APK #

# START %%PKG_INSTALL%%_SUSE #
# install necessary packages
RUN zypper -nq update \
 && zypper -nq install tar unzip \
 && zypper -nq clean \
 && rm -rf /var/cache/zypp/*
# END %%PKG_INSTALL%%_SUSE #
