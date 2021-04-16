#!/usr/bin/env bash
#
#Copyright (c) 2019, 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

if [[ -f /etc/os-release ]]; then
  cat /etc/os-release | grep -oE '^ID=[\"]?(\w+)[\"]?'
  cat /etc/os-release | grep -oE '^VERSION_ID=[\"]?[[:digit:]\.]+[\"]?'
fi

if command -v dnf &> /dev/null; then
  echo PACKAGE_MANAGER=DNF
elif command -v yum &> /dev/null; then
  echo PACKAGE_MANAGER=YUM
elif command -v microdnf &> /dev/null; then
  echo PACKAGE_MANAGER=MICRODNF
elif command -v apt-get &> /dev/null; then
  echo PACKAGE_MANAGER=APTGET
elif command -v apk &> /dev/null; then
  echo PACKAGE_MANAGER=APK
elif command -v zypper &> /dev/null; then
  echo PACKAGE_MANAGER=ZYPPER
fi

if [[ -n "$JAVA_HOME" ]]; then
  echo JAVA_HOME="$JAVA_HOME"
  echo JAVA_PATH="$(readlink -f $JAVA_HOME)"
fi

if [[ -n "$ORACLE_HOME" ]]; then
  echo ORACLE_HOME="$ORACLE_HOME"
  WLS_TYPE=$(cat "$ORACLE_HOME"/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
  if [[ -n "$WLS_TYPE" ]]; then
    echo WLS_TYPE="$WLS_TYPE"
  fi
  if [[ -n "$JAVA_HOME" ]]; then
    echo WLS_VERSION="$("$JAVA_HOME"/bin/java -cp "$ORACLE_HOME"/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi
  echo OPATCH_VERSION="$("$ORACLE_HOME"/OPatch/opatch version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)')"
  "$ORACLE_HOME"/OPatch/opatch lsinventory > /tmp/lsout 2> /dev/null

  echo ORACLE_HOME_USER="$(stat -c '%U' "$ORACLE_HOME")"
  echo ORACLE_HOME_GROUP="$(stat -c '%G' "$ORACLE_HOME")"

  if [[  -f "/tmp/lsout" ]]; then
    echo LSINV_TEXT="$(base64 -w 0 /tmp/lsout)"
  fi
fi

if [[ -n "$DOMAIN_HOME" ]]; then
  echo DOMAIN_HOME="$DOMAIN_HOME"
  if [[ ! -d "$DOMAIN_HOME" ]] || [[ -z "$(ls -A $DOMAIN_HOME)" ]]; then
    echo WDT_MODEL_ONLY=TRUE
  fi
fi

if [[ -n "$WDT_MODEL_HOME" ]]; then
  echo WDT_MODEL_HOME="$WDT_MODEL_HOME"
fi