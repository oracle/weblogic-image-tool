#
# Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# This file is not a fully functional Dockerfile. Do not use this directly.

# START %%WDT_INSTALL%% #
FROM OS_UPDATE as WDT_BUILD

ARG JAVA_HOME=/u01/jdk
ARG ORACLE_HOME=/u01/oracle
ARG WDT_PKG
ARG WDT_MODEL
ARG DOMAIN_TYPE
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
ARG RCU_RUN_FLAG

#RUN echo ${WLS_PKG} ${JAVA_PKG} ${WDT_MODEL} ${OTMPDIR}

ENV WDT_PKG=${WDT_PKG:-weblogic-deploy.zip} \
    ADMIN_NAME=${ADMIN_NAME:-admin-server} \
    ADMIN_HOST=${ADMIN_HOST:-wlsadmin} \
    ADMIN_PORT=${ADMIN_PORT:-7001} \
    MANAGED_SERVER_NAME=${MANAGED_SERVER_NAME:-} \
    MANAGED_SERVER_PORT=${MANAGED_SERVER_PORT:-8001} \
    WDT_MODEL=${WDT_MODEL:-} \
    WLSDEPLOY_PROPERTIES="-Djava.security.egd=file:/dev/./urandom" \
    DOMAIN_TYPE=${DOMAIN_TYPE:-WLS} \
    DOMAIN_PARENT=${DOMAIN_PARENT:-/u01/domains} \
    WDT_ARCHIVE=${WDT_ARCHIVE:-} \
    WDT_VARIABLE=${WDT_VARIABLE:-} \
    LC_ALL=${DEFAULT_LOCALE:-en_US.UTF-8} \
    PROPERTIES_FILE_DIR=$ORACLE_HOME/properties \
    WDT_HOME=${WDT_HOME:-/u01/app/weblogic-deploy} \
    SCRIPTS_DIR=${SCRIPTS_DIR:-scripts} \
    OTMPDIR=${OTMPDIR:-/tmp/delme} \
    RCU_RUN_FLAG=${RCU_RUN_FLAG:-}

# DO NOT COMBINE THESE BLOCKS. It won't work when formatting variables like DOMAIN_HOME
ENV DOMAIN_HOME=${DOMAIN_HOME} \
    PATH=$PATH:${JAVA_HOME}/bin:${ORACLE_HOME}/oracle_common/common/bin:${ORACLE_HOME}/wlserver/common/bin:${DOMAIN_HOME}/bin:${ORACLE_HOME}

COPY --from=JDK_BUILD --chown=oracle:oracle $JAVA_HOME $JAVA_HOME/
COPY --from=WLS_BUILD --chown=oracle:oracle $ORACLE_HOME $ORACLE_HOME/
COPY --chown=oracle:oracle ${WDT_PKG} ${WDT_MODEL} ${WDT_ARCHIVE} ${WDT_VARIABLE} ${OTMPDIR}/
#COPY --chown=oracle:oracle ${SCRIPTS_DIR}/*.sh ${SCRIPT_HOME}/

USER oracle

RUN unzip $OTMPDIR/$WDT_PKG -d $(dirname $WDT_HOME) \
 && mkdir -p $(dirname ${DOMAIN_HOME}) \
 && mkdir -p ${PROPERTIES_FILE_DIR} \
 && if [ -n "$WDT_MODEL" ]; then MODEL_OPT="-model_file ${OTMPDIR}/${WDT_MODEL##*/}"; fi \
 && if [ -n "$WDT_ARCHIVE" ]; then ARCHIVE_OPT="-archive_file ${OTMPDIR}/${WDT_ARCHIVE##*/}"; fi \
 && if [ -n "$WDT_VARIABLE" ]; then VARIABLE_OPT="-variable_file ${OTMPDIR}/${WDT_VARIABLE##*/}"; fi \
 && if [ -n "${RCU_RUN_FLAG}" ]; then RCU_RUN_OPT="-run_rcu"; fi \
 && cd ${WDT_HOME}/bin \
 && ${WDT_HOME}/bin/createDomain.sh \
 -oracle_home ${ORACLE_HOME} \
 -java_home ${JAVA_HOME} \
 -domain_home ${DOMAIN_HOME} \
 -domain_type ${DOMAIN_TYPE} \
 $RCU_RUN_OPT \
 $VARIABLE_OPT \
 $MODEL_OPT \
 $ARCHIVE_OPT \
 && rm -rf ${JAVA_HOME} ${ORACLE_HOME} ${WDT_HOME} $OTMPDIR

# END %%WDT_INSTALL%% #

# START %%WDT_CMD%% #
# Expose admin server, managed server port
EXPOSE $ADMIN_PORT $MANAGED_SERVER_PORT
#CMD ["sh", "-c", "${DOMAIN_HOME}/startAdminServer.sh"]

# END %%WDT_CMD%% #

# START %%WDT_COPY_DOMAIN%% #
COPY --from=WDT_BUILD --chown=oracle:oracle $DOMAIN_HOME $DOMAIN_HOME/
# END %%WDT_COPY_DOMAIN%% #

# START %%OPATCH_1394_COPY%% #
COPY --chown=oracle:oracle p28186730_139400_Generic.zip $OTMPDIR/opatch/
# END %%OPATCH_1394_COPY%% #

# START %%CREATE_OPATCH_1394%% #
 && cd $OTMPDIR/opatch \
 && $JAVA_HOME/bin/jar -xf $OTMPDIR/opatch/p28186730_139400_Generic.zip \
 && $JAVA_HOME/bin/java -jar $OTMPDIR/opatch/6880880/opatch_generic.jar -silent oracle_home=$ORACLE_HOME \
# END %%CREATE_OPATCH_1394%% #

# START %%UPDATE_OPATCH_1394%% #
RUN cd $OTMPDIR/opatch \
 && $JAVA_HOME/bin/jar -xf $OTMPDIR/opatch/p28186730_139400_Generic.zip \
 && $JAVA_HOME/bin/java -jar $OTMPDIR/opatch/6880880/opatch_generic.jar -silent oracle_home=$ORACLE_HOME \
 && rm -rf $OTMPDIR
# END %%UPDATE_OPATCH_1394%% #

# START %%PATCH_APPLY_COPY%% #
COPY --chown=oracle:oracle $PATCHDIR/* $OTMPDIR/patches/
# END %%PATCH_APPLY_COPY%% #

# START %%CREATE_PATCH_APPLY%% #
 && $ORACLE_HOME/OPatch/opatch napply -silent -oh $ORACLE_HOME -phBaseDir $OTMPDIR/patches \
 && $ORACLE_HOME/OPatch/opatch util cleanup -silent -oh $ORACLE_HOME \
# END %%CREATE_PATCH_APPLY%% #

# START %%UPDATE_PATCH_APPLY%% #
RUN $ORACLE_HOME/OPatch/opatch napply -silent -oh $ORACLE_HOME -phBaseDir $OTMPDIR/patches \
 && $ORACLE_HOME/OPatch/opatch util cleanup -silent -oh $ORACLE_HOME \
 && rm -rf $OTMPDIR
# END %%UPDATE_PATCH_APPLY%% #

# START %%PKG_INSTALL%%_YUM #
RUN yum -y --downloaddir=$OTMPDIR install gzip tar unzip \
 && yum -y --downloaddir=$OTMPDIR clean all \
 && rm -rf $OTMPDIR
# END %%PKG_INSTALL%%_YUM #

# START %%PKG_INSTALL%%_APT #
RUN apt-get -y update \
 && apt-get -y upgrade \
 && apt-get -y install gzip tar unzip \
 && apt-get -y clean all
# END %%PKG_INSTALL%%_APT #

# START %%PKG_INSTALL%%_APK #
RUN apk update \
 && apk upgrade \
 && apk add gzip tar unzip \
 && rm -rf /var/cache/apk/*
# END %%PKG_INSTALL%%_APK #

# START %%PKG_INSTALL%%_SUSE #
RUN zypper -nq update \
 && zypper -nq install gzip tar unzip \
 && zypper -nq clean \
 && rm -rf /var/cache/zypp/*
# END %%PKG_INSTALL%%_SUSE #

# START %%PKG_UPDATE%%_YUM #
RUN yum -y --downloaddir=$OTMPDIR update \
 && yum -y --downloaddir=$OTMPDIR clean all \
 && rm -rf $OTMPDIR
# END %%PKG_UPDATE%%_YUM #

# START %%PKG_UPDATE%%_APT #
RUN apt-get -y update \
 && apt-get -y upgrade \
 && apt-get -y clean all
# END %%PKG_UPDATE%%_APT #

# START %%PKG_UPDATE%%_APK #
RUN apk update \
 && apk upgrade \
 && rm -rf /var/cache/apk/*
# END %%PKG_UPDATE%%_APK #

# START %%PKG_UPDATE%%_SUSE #
RUN zypper -nq update \
 && zypper -nq clean \
 && rm -rf /var/cache/zypp/*
# END %%PKG_UPDATE%%_SUSE #