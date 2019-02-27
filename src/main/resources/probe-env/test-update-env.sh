#!/usr/bin/env bash
#
#Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
#
#Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#

if [[ -f /etc/os-release ]]; then
  cat /etc/os-release | grep -oE '^ID=[\"]?(\w+)[\"]?'
  cat /etc/os-release | grep -oE '^VERSION_ID=[\"]?[[:digit:]\.]+[\"]?'
fi

if [[ ! -z "$ORACLE_HOME" ]]; then
  echo ORACLE_HOME="$ORACLE_HOME"
  WLS_TYPE=$(cat $ORACLE_HOME/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
  if [[ ! -z "$WLS_TYPE" ]]; then
    echo WLS_TYPE="$WLS_TYPE"
  fi
  if [[ ! -z "$JAVA_HOME" ]]; then
    echo WLS_VERSION=$($JAVA_HOME/bin/java -cp $ORACLE_HOME/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)
  fi
  echo OPATCH_VERSION=$($ORACLE_HOME/OPatch/opatch version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)')
  LSINV_TEXT=$($ORACLE_HOME/OPatch/opatch lsinventory 2> /dev/null)
  if [[ ! -z "$LSINV_TEXT" ]]; then
    echo "$LSINV_TEXT" > /tmp_scripts/opatch-lsinventory.txt 2> /dev/null
  fi
fi

#if [[ -f /etc/os-release ]]; then
#  echo OS_ID=$(cat /etc/os-release | grep -oP '^ID=\"\K\w+(?=\")')
#  echo OS_VERSION_ID=$(cat /etc/os-release | grep -oP '^VERSION_ID=\"\K[\d\.]+(?=\")')
#fi
#
#if [[ ! -z "$ORACLE_HOME" ]]; then
#  echo ORACLE_HOME="$ORACLE_HOME"
#  WLS_TYPE=$(cat $ORACLE_HOME/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
#  if [[ ! -z "$WLS_TYPE" ]]; then
#    echo WLS_TYPE="$WLS_TYPE"
#  fi
#  if [[ ! -z "$JAVA_HOME" ]]; then
#    echo WLS_VERSION=$($JAVA_HOME/bin/java -cp $ORACLE_HOME/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oP 'WebLogic Server \K[\d\.]+\s+')
#  fi
#  echo OPATCH_VERSION=$($ORACLE_HOME/OPatch/opatch version 2> /dev/null | grep -oP '\s+\K[\d\.]+$')
#  $ORACLE_HOME/OPatch/opatch lsinventory > /tmp_scripts/opatch-lsinventory.xml 2>&1
#fi

#echo ORACLE_HOME=$ORACLE_HOME
#echo WLS_TYPE=$(cat $ORACLE_HOME/inventory/registry.xml 2> /dev/null | perl -ne '/WebLogic Server for FMW/ && print "fmw"')
#echo WLS_VERSION=$($JAVA_HOME/bin/java -cp $ORACLE_HOME/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | perl -ne '/\s+([\d\.]+)\s+/ && print $1')
#echo OPATCH_VERSION=$($ORACLE_HOME/OPatch/opatch version 2> /dev/null | perl -n -e'/\s+([\d\.]+)\s+/ && print $1')
#echo OS_ID=$(cat /etc/os-release 2> /dev/null | perl -ne '/^ID=\"(\S+)\"$/ && print $1')
#echo OS_VERSION_ID=$(cat /etc/os-release 2> /dev/null | perl -ne '/^VERSION_ID=\"(\S+)\"$/ && print $1')
#$ORACLE_HOME/OPatch/opatch lsinventory > /tmp_scripts/opatch-lsinventory.xml 2>&1