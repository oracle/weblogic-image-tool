#!/bin/bash
#
#Copyright (c) 2019, 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
# imagetool.sh

set -e

#
# Make sure that the JAVA_HOME environment variable is set to point to a
# JDK 8 or higher JVM (and that it isn't OpenJDK).
#
if [ -z "${JAVA_HOME}" ]; then
  echo "Please set the JAVA_HOME environment variable to match the location of your Java installation. Java 8 or newer is required." >&2
  exit -1
elif [ ! -d "${JAVA_HOME}" ]; then
  echo "Your JAVA_HOME environment variable points to a non-existent directory: ${JAVA_HOME}" >&2
  exit -1
fi

if [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_EXE=${JAVA_HOME}/bin/java
else
  echo "Java executable at ${JAVA_HOME}/bin/java either does not exist or is not executable" >&2
  exit -1
fi

read_link() {
  PREV_DIR=`pwd`
  CHASE_LINK=$1
  cd `dirname $CHASE_LINK`
  CHASE_LINK=`basename $CHASE_LINK`
  while [ -L "$CHASE_LINK" ]
  do
    CHASE_LINK=`readlink $CHASE_LINK`
    cd `dirname $CHASE_LINK`
    CHASE_LINK=`basename $CHASE_LINK`
  done
  _DIR=`pwd -P`
  RESULT_PATH=$_DIR/$CHASE_LINK
  cd $PREV_DIR
  echo $RESULT_PATH
}

script_dir=$( dirname "$( read_link "${BASH_SOURCE[0]}" )" )
IMAGETOOL_HOME=$(cd "${script_dir}/.." ; pwd)
export IMAGETOOL_HOME
${JAVA_HOME}/bin/java -cp "${IMAGETOOL_HOME}/lib/*" -Djava.util.logging.config.file=${IMAGETOOL_HOME}/bin/logging.properties com.oracle.weblogic.imagetool.cli.ImageTool $@

