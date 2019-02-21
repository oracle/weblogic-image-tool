#!/usr/bin/env bash

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
fi

#if [[ -f /etc/os-release ]]; then
#  echo OS_ID=$(cat /etc/os-release | grep -oP '^ID=[\"]?\K\w+(?=[\"]?)')
#  echo OS_VERSION_ID=$(cat /etc/os-release | grep -oP '^VERSION_ID=[\"]?\K[\d\.]+(?=[\"]?)')
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
#fi

#echo WLS_TYPE=$(cat $ORACLE_HOME/inventory/registry.xml 2> /dev/null | perl -ne '/WebLogic Server for FMW/ && print "fmw"')
#echo WLS_VERSION=$($JAVA_HOME/bin/java -cp $ORACLE_HOME/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | perl -ne '/\s+([\d\.]+)\s+/ && print $1')
#echo OS_ID=$(cat /etc/os-release 2> /dev/null | perl -ne '/^ID=\"(\S+)\"$/ && print $1')
#echo OS_VERSION_ID=$(cat /etc/os-release 2> /dev/null | perl -ne '/^VERSION_ID=\"(\S+)\"$/ && print $1')
