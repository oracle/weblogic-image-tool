#
#Copyright (c) 2019, 2020, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
# source setup.sh
umask 27
#
# Make sure that the JAVA_HOME environment variable is set to point to a
# JDK 8 or higher JVM (and that it isn't OpenJDK).
#
if [ -z "${JAVA_HOME}" ]; then
  echo "JAVA_HOME not set. Please set the JAVA_HOME environment variable to match the location of your Java 8 or 11 installation." >&2
  return
elif [ ! -d "${JAVA_HOME}" ]; then
  echo "Your JAVA_HOME environment variable points to a non-existent directory: ${JAVA_HOME}" >&2
  return
fi

if [ -x "${JAVA_HOME}/bin/java" ]; then
  JAVA_EXE=${JAVA_HOME}/bin/java
else
  echo "Java executable at ${JAVA_HOME}/bin/java either does not exist or is not executable" >&2
  return
fi

function read_link() {
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

unalias imagetool 2> /dev/null
script_dir=$( dirname "$( read_link "${BASH_SOURCE[0]}" )" )
IMAGETOOL_HOME=`cd "${script_dir}/.." ; pwd`
export IMAGETOOL_HOME
alias imagetool="${JAVA_HOME}/bin/java -cp \"${IMAGETOOL_HOME}/lib/*\" -Djava.util.logging.config.file=${IMAGETOOL_HOME}/bin/logging.properties com.oracle.weblogic.imagetool.cli.ImageTool"
source ${IMAGETOOL_HOME}/lib/imagetool_completion.sh
