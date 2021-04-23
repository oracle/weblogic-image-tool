#!/usr/bin/env bash
#
#Copyright (c) 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#
for token in $(java -version 2>&1)
do
  if [[ $token =~ \"([[:digit:]])\.([[:digit:]])\.(.*)\" ]]
  then
    echo javaVersion="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
  fi
done

if command -v dnf &> /dev/null; then
  echo packageManager=dnf
elif command -v yum &> /dev/null; then
  echo packageManager=yum
elif command -v microdnf &> /dev/null; then
  echo packageManager=microdnf
elif command -v apt-get &> /dev/null; then
  echo packageManager=aptget
elif command -v apk &> /dev/null; then
  echo packageManager=apk
elif command -v zypper &> /dev/null; then
  echo packageManager=zypper
fi

if [[ -n "$JAVA_HOME" ]]; then
  echo javaHome="$JAVA_HOME"
fi

if [[ -n "$ORACLE_HOME" ]]; then
  echo oracleHome="$ORACLE_HOME"
  WLS_TYPE=$(cat "$ORACLE_HOME"/inventory/registry.xml 2> /dev/null | grep -q 'WebLogic Server for FMW' && printf "fmw")
  if [[ -n "$WLS_TYPE" ]]; then
    echo wlsType="$WLS_TYPE"
  fi
  if [[ -n "$JAVA_HOME" ]]; then
    echo wlsVersion="$("$JAVA_HOME"/bin/java -cp "$ORACLE_HOME"/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi
  echo oracleHomeUser="$(stat -c '%U' "$ORACLE_HOME")"
  echo oracleHomeGroup="$(stat -c '%G' "$ORACLE_HOME")"

  echo opatchVersion="$("$ORACLE_HOME"/OPatch/opatch version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)')"
  echo oraclePatches=$("$ORACLE_HOME"/OPatch/opatch lspatches | awk -F";" '/^[0-9]/ {print $0";"}')
fi

if [[ -n "$DOMAIN_HOME" ]]; then
  echo domainHome="$DOMAIN_HOME"
  if [[ ! -d "$DOMAIN_HOME" ]] || [[ -z "$(ls -A $DOMAIN_HOME)" ]]; then
    echo wdtModelOnly=true
  fi
fi

if [[ -n "$WDT_MODEL_HOME" ]]; then
  echo wdtModelHome="$WDT_MODEL_HOME"
fi
