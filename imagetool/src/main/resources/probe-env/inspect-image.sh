#!/bin/sh
#
#Copyright (c) 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
if [ "$(type -p java)" ]; then
  echo javaVersion="$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}')"
fi

if [ "$(type -p dnf)" ]; then
  echo packageManager=DNF
elif [ "$(type -p yum)" ]; then
  echo packageManager=YUM
elif [ "$(type -p microdnf)" ]; then
  echo packageManager=MICRODNF
elif [ "$(type -p apt-get)" ]; then
  echo packageManager=APTGET
elif [ "$(type -p apk)" ]; then
  echo packageManager=APK
elif [ "$(type -p zypper)" ]; then
  echo packageManager=ZYPPER
fi

if [ -n "$JAVA_HOME" ]; then
  echo javaHome="$JAVA_HOME"
fi

if [ -n "$DOMAIN_HOME" ]; then
  echo domainHome="$DOMAIN_HOME"
  if [ ! -d "$DOMAIN_HOME" ] || [ -z "$(ls -A $DOMAIN_HOME)" ]; then
    echo wdtModelOnly=true
  fi
fi

if [ -n "$WDT_MODEL_HOME" ]; then
  echo wdtModelHome="$WDT_MODEL_HOME"
fi


if [ -n "$ORACLE_HOME" ]; then
  echo oracleHome="$ORACLE_HOME"
  WLS_TYPE=$(cat "$ORACLE_HOME"/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
  if [ -n "$WLS_TYPE" ]; then
    echo wlsType="$WLS_TYPE"
  fi
  if [ -n "$JAVA_HOME" ]; then
    echo wlsVersion="$("$JAVA_HOME"/bin/java -cp "$ORACLE_HOME"/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi

  echo oracleHomeUser="$(stat -c '%U' "$ORACLE_HOME")"
  echo oracleHomeGroup="$(stat -c '%G' "$ORACLE_HOME")"

  echo oracleInstalledProducts="$(awk -F\" '{ORS=","} /product-family/ { print $2 }' $ORACLE_HOME/inventory/registry.xml | sed 's/,$//')"
fi
