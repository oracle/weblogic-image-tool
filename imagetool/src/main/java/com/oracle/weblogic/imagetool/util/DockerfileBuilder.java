package com.oracle.weblogic.imagetool.util;

import java.io.*;
import java.util.List;

/**
 * Provide a programmatic way of creating a Dockerfile for the WebLogic Image Tool.
 */
public class DockerfileBuilder {

    private final String[] OPTION_SKIPPED = new String[] {};

    private final String[] COPYRIGHT_NOTICE = new String[]{
            "#",
            "# Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.",
            "#",
            "# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.",
            "#",
            "#"
    };

    private final String[] LINUX_BASE_IMAGE = new String[]{
            "ARG BASE_IMAGE=oraclelinux:7-slim",
            "FROM ${BASE_IMAGE} as OS_UPDATE"
    };

    private final String[] USER_ROOT = new String[]{
            "USER root"
    };

    private final String[] CREATE_ORACLE_USER = new String[]{
            "## Create oracle user",
            "RUN if [ -z \"$(getent group oracle)\" ]; then hash groupadd &> /dev/null && groupadd -g 1000 oracle || exit -1 ; fi \\",
            " && if [ -z \"$(getent passwd oracle)\" ]; then hash useradd &> /dev/null && useradd -u 1100 -g oracle oracle || exit -1; fi \\",
            " && mkdir /u01 \\",
            " && chown oracle:oracle /u01"
    };

    private final String[] PACKAGE_INSTALL_YUM = new String[] {
            "## Install packages needed for installers",
            "RUN yum -y --downloaddir=$OTMPDIR install gzip tar unzip \\",
            " && yum -y --downloaddir=$OTMPDIR clean all \\",
            " && rm -rf $OTMPDIR"
    };

    private final String[] INSTALL_JDK = new String[] {
            "FROM OS_UPDATE as JDK_BUILD",
            "ARG JAVA_PKG",
            "ARG JAVA_HOME=/u01/jdk",
            "ARG OTMPDIR",
            "",
            "ENV JAVA_PKG=${JAVA_PKG:-server-jre-*-linux-x64.tar.gz} \\",
            "    JAVA_HOME=${JAVA_HOME:-/u01/jdk} \\",
            "    OTMPDIR=${OTMPDIR:-/tmp/delme}",
            "COPY --chown=oracle:oracle $JAVA_PKG $OTMPDIR/",
            "",
            "USER oracle",
            "",
            "RUN tar xzvf $OTMPDIR/$JAVA_PKG -C /u01 \\",
            " && mv /u01/jdk* $JAVA_HOME \\",
            " && rm -rf $OTMPDIR"
    };

    private final String[] FMW_INSTALL_PREPARE_STEP = new String[] {
            "FROM OS_UPDATE as WLS_BUILD",
            "ARG WLS_PKG",
            "ARG JAVA_HOME=/u01/jdk",
            "ARG INV_LOC",
            "ARG WLS_RESP",
            "ARG ORACLE_HOME=/u01/oracle",
            "ARG ORAINST",
            "ARG OTMPDIR",
            "ARG PATCHDIR",
            "",
            "ENV WLS_PKG=${WLS_PKG:-fmw_12.2.1.3.0_wls_Disk1_1of1.zip} \\",
            "    JAVA_HOME=${JAVA_HOME:-/u01/jdk} \\",
            "    ORACLE_HOME=${ORACLE_HOME:-/u01/oracle} \\",
            "    INV_LOC=${INV_LOC:-/u01/oracle/oraInventory} \\",
            "    WLS_RESP=${WLS_RESP:-wls.rsp} \\",
            "    ORAINST=${ORAINST:-oraInst.loc} \\",
            "    OTMPDIR=${OTMPDIR:-/tmp/delme} \\",
            "    OPATCH_NO_FUSER=true \\",
            "    PATCHDIR=${PATCHDIR:-patches}",
            "",
            "# Install base WLS",
            "COPY --from=JDK_BUILD --chown=oracle:oracle $JAVA_HOME $JAVA_HOME/",
            "COPY --chown=oracle:oracle $WLS_PKG $WLS_RESP $OTMPDIR/",
            "COPY --chown=oracle:oracle $ORAINST $INV_LOC/"
    };

    private final String[] FMW_INSTALL = new String[] {
            "USER oracle",
            "",
            "RUN unzip $OTMPDIR/$WLS_PKG -d $OTMPDIR \\",
            " && $JAVA_HOME/bin/java -Xmx1024m -jar $OTMPDIR/fmw_*.jar -silent ORACLE_HOME=$ORACLE_HOME \\",
            "    -responseFile $OTMPDIR/$WLS_RESP -invPtrLoc $INV_LOC/$ORAINST -ignoreSysPrereqs -force -novalidation"
//          " && rm -rf $JAVA_HOME $OTMPDIR"
    };

    private final String[] FINAL_BUILD = new String[] {
            "FROM ${BASE_IMAGE} as FINAL_BUILD",
            "",
            "ARG JAVA_HOME=/u01/jdk",
            "ARG ORACLE_HOME=/u01/oracle",
            "ARG DOMAIN_PARENT",
            "ARG DOMAIN_HOME",
            "ARG ADMIN_NAME",
            "ARG ADMIN_HOST",
            "ARG ADMIN_PORT",
            "ARG MANAGED_SERVER_PORT",
            "",
            "ENV ORACLE_HOME=${ORACLE_HOME} \\",
            "    JAVA_HOME=${JAVA_HOME} \\",
            "    ADMIN_NAME=${ADMIN_NAME:-admin-server} \\",
            "    ADMIN_HOST=${ADMIN_HOST:-wlsadmin} \\",
            "    ADMIN_PORT=${ADMIN_PORT:-7001} \\",
            "    MANAGED_SERVER_NAME=${MANAGED_SERVER_NAME:-} \\",
            "    MANAGED_SERVER_PORT=${MANAGED_SERVER_PORT:-8001} \\",
            "    WLSDEPLOY_PROPERTIES=\"-Djava.security.egd=file:/dev/./urandom\" \\",
            "    DOMAIN_PARENT=${DOMAIN_PARENT:-/u01/domains} \\",
            "    LC_ALL=${DEFAULT_LOCALE:-en_US.UTF-8} \\",
            "    PROPERTIES_FILE_DIR=$ORACLE_HOME/properties",
            "",
            "# DO NOT COMBINE THESE BLOCKS. It won't work when formatting variables like DOMAIN_HOME",
            "ENV DOMAIN_HOME=${DOMAIN_HOME:-/u01/domains/base_domain} \\",
            "    PROPERTIES_FILE_DIR=$ORACLE_HOME/properties \\",
            "    PATH=$PATH:${JAVA_HOME}/bin:${ORACLE_HOME}/oracle_common/common/bin:${ORACLE_HOME}/wlserver/common/bin:${DOMAIN_HOME}/bin:${ORACLE_HOME}",
            "",
            "## Create oracle user",
            "RUN if [ -z \"$(getent group oracle)\" ]; then hash groupadd &> /dev/null && groupadd -g 1000 oracle || exit -1 ; fi \\",
            " && if [ -z \"$(getent passwd oracle)\" ]; then hash useradd &> /dev/null && useradd -u 1100 -g oracle oracle || exit -1; fi \\",
            " && mkdir -p $(dirname $JAVA_HOME) $(dirname $ORACLE_HOME) $(dirname $DOMAIN_HOME) \\",
            " && chown oracle:oracle $(dirname $JAVA_HOME) $(dirname $ORACLE_HOME) $(dirname $DOMAIN_HOME)",
            "",
            "COPY --from=JDK_BUILD --chown=oracle:oracle $JAVA_HOME $JAVA_HOME/",
            "COPY --from=WLS_BUILD --chown=oracle:oracle $ORACLE_HOME $ORACLE_HOME/"
    };

    private final String[] CLOSING_STATEMENTS = new String[] {
            "USER oracle",
            "WORKDIR $ORACLE_HOME"
    };

    private final List<String> optionFlags;

    public DockerfileBuilder(final List<String> params) {
        optionFlags = params;
    }


    private String[] optionalYumPackageInstall() {
        if (optionFlags.contains(Constants.YUM)) {
            return PACKAGE_INSTALL_YUM;
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalOpatchPatchPrepare() {
        if (optionFlags.contains(Constants.OPATCH_PATCH)) {
            return new String[]{
                    "COPY --chown=oracle:oracle p28186730_139400_Generic.zip $OTMPDIR/opatch/"
            };
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalOpatchPatchInstall() {
        if (optionFlags.contains(Constants.OPATCH_PATCH)) {
            return new String[]{
                    "RUN cd $OTMPDIR/opatch \\",
                    " && $JAVA_HOME/bin/jar -xf $OTMPDIR/opatch/p28186730_139400_Generic.zip \\",
                    " && $JAVA_HOME/bin/java -jar $OTMPDIR/opatch/6880880/opatch_generic.jar -silent -ignoreSysPrereqs -force -novalidation oracle_home=$ORACLE_HOME \\"
            };
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalApplyPatchesPrepare() {
        if (optionFlags.contains(Constants.PATCH)) {
            return new String[] {
                    "COPY --chown=oracle:oracle $PATCHDIR/* $OTMPDIR/patches/"
            };
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalApplyPatchesInstall() {
        if (optionFlags.contains(Constants.PATCH)) {
            return new String[] {
                    "RUN $ORACLE_HOME/OPatch/opatch napply -silent -oh $ORACLE_HOME -phBaseDir $OTMPDIR/patches \\",
                    " && $ORACLE_HOME/OPatch/opatch util cleanup -silent -oh $ORACLE_HOME"
            };
        }

        return OPTION_SKIPPED;
    }


    /**
     * Thinking of changing the download WDT latest to:
     * curl -s https://api.github.com/repos/oracle/weblogic-deploy-tooling/releases/latest|grep browser_download | grep "browser_download_url" | cut -d : -f 2,3 | tr -d \" | wget -qi -
     */
    private String[] optionalInstallWdt() {
        //This has A LOT of room for improvement.  Copying what was already there, but this needs to be streamlined.
        if (optionFlags.contains(Constants.WDT)) {
            return new String[]{
                    "FROM OS_UPDATE as WDT_BUILD",
                    "",
                    "ARG JAVA_HOME=/u01/jdk",
                    "ARG ORACLE_HOME=/u01/oracle",
                    "ARG WDT_PKG",
                    "ARG WDT_MODEL",
                    "ARG DOMAIN_TYPE",
                    "ARG DOMAIN_PARENT",
                    "ARG DOMAIN_HOME",
                    "ARG WDT_ARCHIVE",
                    "ARG WDT_VARIABLE",
                    "ARG ADMIN_NAME",
                    "ARG ADMIN_HOST",
                    "ARG ADMIN_PORT",
                    "ARG MANAGED_SERVER_PORT",
                    "ARG SCRIPTS_DIR",
                    "ARG WDT_HOME",
                    "ARG RCU_RUN_FLAG",
                    "",
                    "ENV WDT_PKG=${WDT_PKG:-weblogic-deploy.zip} \\",
                    "    ADMIN_NAME=${ADMIN_NAME:-admin-server} \\",
                    "    ADMIN_HOST=${ADMIN_HOST:-wlsadmin} \\",
                    "    ADMIN_PORT=${ADMIN_PORT:-7001} \\",
                    "    MANAGED_SERVER_NAME=${MANAGED_SERVER_NAME:-} \\",
                    "    MANAGED_SERVER_PORT=${MANAGED_SERVER_PORT:-8001} \\",
                    "    WDT_MODEL=${WDT_MODEL:-} \\",
                    "    WLSDEPLOY_PROPERTIES=\"-Djava.security.egd=file:/dev/./urandom\" \\",
                    "    DOMAIN_TYPE=${DOMAIN_TYPE:-WLS} \\",
                    "    DOMAIN_PARENT=${DOMAIN_PARENT:-/u01/domains} \\",
                    "    WDT_ARCHIVE=${WDT_ARCHIVE:-} \\",
                    "    WDT_VARIABLE=${WDT_VARIABLE:-} \\",
                    "    LC_ALL=${DEFAULT_LOCALE:-en_US.UTF-8} \\",
                    "    PROPERTIES_FILE_DIR=$ORACLE_HOME/properties \\",
                    "    WDT_HOME=${WDT_HOME:-/u01/app/weblogic-deploy} \\",
                    "    SCRIPTS_DIR=${SCRIPTS_DIR:-scripts} \\",
                    "    OTMPDIR=${OTMPDIR:-/tmp/delme} \\",
                    "    RCU_RUN_FLAG=${RCU_RUN_FLAG:-}",
                    "",
                    "# DO NOT COMBINE THESE BLOCKS. It won't work when formatting variables like DOMAIN_HOME",
                    "ENV DOMAIN_HOME=${DOMAIN_HOME:-/u01/domains/base_domain} \\",
                    "    PATH=$PATH:${JAVA_HOME}/bin:${ORACLE_HOME}/oracle_common/common/bin:${ORACLE_HOME}/wlserver/common/bin:${DOMAIN_HOME}/bin:${ORACLE_HOME}",
                    "",
                    "COPY --from=JDK_BUILD --chown=oracle:oracle $JAVA_HOME $JAVA_HOME/",
                    "COPY --from=WLS_BUILD --chown=oracle:oracle $ORACLE_HOME $ORACLE_HOME/",
                    "COPY --chown=oracle:oracle ${WDT_PKG} ${WDT_MODEL} ${WDT_ARCHIVE} ${WDT_VARIABLE} ${OTMPDIR}/",
                    "#COPY --chown=oracle:oracle ${SCRIPTS_DIR}/*.sh ${SCRIPT_HOME}/",
                    "",
                    "USER oracle",
                    "",
                    "RUN unzip $OTMPDIR/$WDT_PKG -d $(dirname $WDT_HOME) \\",
                    " && mkdir -p $(dirname ${DOMAIN_HOME}) \\",
                    " && mkdir -p ${PROPERTIES_FILE_DIR} \\",
                    " && if [ -n \"$WDT_MODEL\" ]; then MODEL_OPT=\"-model_file ${OTMPDIR}/${WDT_MODEL##*/}\"; fi \\",
                    " && if [ -n \"$WDT_ARCHIVE\" ]; then ARCHIVE_OPT=\"-archive_file ${OTMPDIR}/${WDT_ARCHIVE##*/}\"; fi \\",
                    " && if [ -n \"$WDT_VARIABLE\" ]; then VARIABLE_OPT=\"-variable_file ${OTMPDIR}/${WDT_VARIABLE##*/}\"; fi \\",
                    " && if [ -n \"${RCU_RUN_FLAG}\" ]; then RCU_RUN_OPT=\"-run_rcu\"; fi \\",
                    " && cd ${WDT_HOME}/bin \\",
                    " && ${WDT_HOME}/bin/createDomain.sh \\",
                    " -oracle_home ${ORACLE_HOME} \\",
                    " -java_home ${JAVA_HOME} \\",
                    " -domain_home ${DOMAIN_HOME} \\",
                    " -domain_type ${DOMAIN_TYPE} \\",
                    " $RCU_RUN_OPT \\",
                    " $VARIABLE_OPT \\",
                    " $MODEL_OPT \\",
                    " $ARCHIVE_OPT \\",
                    " && rm -rf ${JAVA_HOME} ${ORACLE_HOME} ${WDT_HOME} $OTMPDIR"
            };
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalCopyWdtDomain() {
        //assumes that the domain was created previously in temp image so that it can be copied to final image
        if (optionFlags.contains(Constants.WDT)) {
            return new String[] {
                    "COPY --from=WDT_BUILD --chown=oracle:oracle $DOMAIN_HOME $DOMAIN_HOME/"
            };
        }

        return OPTION_SKIPPED;
    }

    private String[] optionalExposeWdtDomain() {
        if (optionFlags.contains(Constants.WDT)) {
            return new String[] {
                    "# Expose admin server, managed server port",
                    "EXPOSE $ADMIN_PORT $MANAGED_SERVER_PORT"
            };
        }

        return OPTION_SKIPPED;
    }


    /**
     * Write the Dockerfile contents to the specified file.
     * @param filename the name of the file to be written.
     * @throws IOException if failures occur in the java.io API, such as FileNotFound
     */
    public void write(String filename) throws IOException{
        PrintWriter output = new PrintWriter( new File(filename) );
        write( output );
        output.flush();
        output.close();
    }

    /**
     * Write the Dockerfile contents to the outputstream.
     * Could be public method, but not currently needed.
     * @param out outputstream to be written
     * @throws IOException if failures occur in the java.io API
     */
    private void write(Writer out) throws IOException {
        writeArray(out, COPYRIGHT_NOTICE);
        writeArray(out, LINUX_BASE_IMAGE);
        writeArray(out, USER_ROOT);
        writeArray(out, optionalYumPackageInstall());
        writeArray(out, CREATE_ORACLE_USER);
        writeArray(out, INSTALL_JDK);
        writeArray(out, FMW_INSTALL_PREPARE_STEP);
        writeArray(out, optionalOpatchPatchPrepare());
        writeArray(out, optionalApplyPatchesPrepare());
        writeArray(out, FMW_INSTALL);
        writeArray(out, optionalOpatchPatchInstall());
        writeArray(out, optionalApplyPatchesInstall());
        writeArray(out, optionalInstallWdt());
        writeArray(out, FINAL_BUILD);
        writeArray(out, optionalCopyWdtDomain());
        writeArray(out, CLOSING_STATEMENTS);
        writeArray(out, optionalExposeWdtDomain());
    }

    private static void writeArray(Writer out, String[] strings) throws IOException {
        for (String s : strings) {
            out.write(s);
            out.write(System.lineSeparator());
        }
    }
}
