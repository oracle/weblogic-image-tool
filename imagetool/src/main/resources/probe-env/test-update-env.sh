#!/usr/bin/env bash
#
#Copyright (c) 2019, 2020, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

if [[ -f /etc/os-release ]]; then
  cat /etc/os-release | grep -oE '^ID=[\"]?(\w+)[\"]?'
  cat /etc/os-release | grep -oE '^VERSION_ID=[\"]?[[:digit:]\.]+[\"]?'
fi

if [[ -n "$JAVA_HOME" ]]; then
  echo JAVA_HOME="$JAVA_HOME"
  echo JAVA_PATH="$(readlink -f $JAVA_HOME)"
  echo ADMIN_PORT="${ADMIN_PORT}"
  echo MANAGED_SERVER_PORT="${MANAGED_SERVER_PORT}"
fi

if [[ -n "$ORACLE_HOME" ]]; then
  echo ORACLE_HOME="$ORACLE_HOME"
  WLS_TYPE=$(cat "$ORACLE_HOME"/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
  if [[ -n "$WLS_TYPE" ]]; then
    echo WLS_TYPE="$WLS_TYPE"
  fi
  if [[ -n "$JAVA_HOME" ]]; then
    echo WLS_VERSION="$("$JAVA_HOME"/bin/java -cp $ORACLE_HOME/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi
  echo OPATCH_VERSION="$("$ORACLE_HOME"/OPatch/opatch version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)')"
  "$ORACLE_HOME"/OPatch/opatch lsinventory > /tmp/lsout 2> /dev/null

  if [[  -f "/tmp/lsout" ]]; then
    echo LSINV_TEXT="$(base64 /tmp/lsout)"
  fi
fi

if [[ -n "$DOMAIN_HOME" ]]; then
  echo DOMAIN_HOME="$DOMAIN_HOME"
fi
