#!/bin/sh
#
#Copyright (c) 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

if type dnf > /dev/null 2>&1; then
  echo packageManager=DNF
elif type yum > /dev/null 2>&1; then
  echo packageManager=YUM
elif type microdnf > /dev/null 2>&1; then
  echo packageManager=MICRODNF
elif type apt-get > /dev/null 2>&1; then
  echo packageManager=APTGET
elif type apk > /dev/null 2>&1; then
  echo packageManager=APK
elif type zypper > /dev/null 2>&1; then
  echo packageManager=ZYPPER
else
  echo packageManager=NONE
fi

if [ -n "$JAVA_HOME" ]; then
  echo javaHome="$JAVA_HOME"
  javaVersion="$("$JAVA_HOME"/bin/java -version 2>&1 | awk -F '\"' '/version/ {print $2}')"
else
  javaVersion="$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}')"
fi

if [ -n "$javaVersion" ]; then
  echo javaVersion="$javaVersion"
fi

if [ -n "$DOMAIN_HOME" ]; then
  echo domainHome="$DOMAIN_HOME"
  if [ ! -d "$DOMAIN_HOME" ] || [ -z "$(ls -A "$DOMAIN_HOME")" ]; then
    echo wdtModelOnly=true
  fi
fi

if [ -n "$WDT_MODEL_HOME" ]; then
  echo wdtModelHome="$WDT_MODEL_HOME"
fi

if [ -n "$WDT_HOME" ]; then
  echo wdtHome="$WDT_HOME"
  echo wdtVersion="$(sed 's/.* //' "$WDT_HOME"/weblogic-deploy/VERSION.txt )"
elif [ -f "/u01/wdt/weblogic-deploy/VERSION.txt" ]; then
  echo wdtHome="/u01/wdt"
  echo wdtVersion="$(sed 's/.* //' /u01/wdt/weblogic-deploy/VERSION.txt)"
fi

if [ -n "$ORACLE_HOME" ]; then
  echo oracleHome="$ORACLE_HOME"

  if [ -n "$JAVA_HOME" ]; then
    echo wlsVersion="$("$JAVA_HOME"/bin/java -cp "$ORACLE_HOME"/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi

  echo oracleHomeUser="$(stat -c '%U' "$ORACLE_HOME")"
  echo oracleHomeGroup="$(stat -c '%G' "$ORACLE_HOME")"

  echo oracleInstalledProducts="$(awk -F\" '{ORS=","} /product-family/ { print $2 }' "$ORACLE_HOME"/inventory/registry.xml | sed 's/,$//')"
fi

echo __OS__arch="$(uname -m)"
if [ -f "/etc/os-release" ]; then
  grep '=' /etc/os-release | sed 's/^/__OS__/'
  releasePackage="$(type rpm >/dev/null 2>&1  && rpm -qf /etc/os-release || echo '')"
  if [ -n "$releasePackage" ]; then
    echo __OS__RELEASE_PACKAGE=$releasePackage
  fi
elif type busybox > /dev/null 2>&1; then
  echo __OS__ID="bb"
  echo __OS__NAME="$(busybox | head -1 | awk '{ print $1 }')"
  echo __OS__VERSION="$(busybox | head -1 | awk '{ print $2 }')"
fi
