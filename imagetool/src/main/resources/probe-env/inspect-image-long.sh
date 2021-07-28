#!/bin/sh
#
#Copyright (c) 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

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
  echo javaVersion="$("$JAVA_HOME"/bin/java -version 2>&1 | awk -F '\"' '/version/ {print $2}')"
else
  echo javaVersion="$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}')"
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

if [ -n "$WDT_HOME" ]; then
  echo wdtHome="$WDT_HOME"
  echo wdtVersion="$(cat $WDT_HOME/weblogic-deploy/VERSION.txt | sed 's/.* //')"
elif [ -f "/u01/wdt/weblogic-deploy/VERSION.txt" ]; then
  echo wdtHome="/u01/wdt"
  echo wdtVersion="$(cat /u01/wdt/weblogic-deploy/VERSION.txt | sed 's/.* //')"
fi

if [ -n "$ORACLE_HOME" ]; then
  echo oracleHome="$ORACLE_HOME"

  if [ -n "$JAVA_HOME" ]; then
    echo wlsVersion="$("$JAVA_HOME"/bin/java -cp "$ORACLE_HOME"/wlserver/server/lib/weblogic.jar weblogic.version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)' | head -1)"
  fi

  echo oracleHomeUser="$(stat -c '%U' "$ORACLE_HOME")"
  echo oracleHomeGroup="$(stat -c '%G' "$ORACLE_HOME")"

  echo opatchVersion="$($ORACLE_HOME/OPatch/opatch version 2> /dev/null | grep -oE -m 1 '([[:digit:]\.]+)')"
  echo oraclePatches="$($ORACLE_HOME/OPatch/opatch lsinventory |
  awk 'BEGIN { ORS=";" }
      /^Unique Patch ID/ { print $4 }
      /^Patch description/ {
        x = substr($0, 21)
        print x
        descriptionNeeded = 0
      }
      /^Patch\s*[0-9]+/ {
        if (descriptionNeeded)
          print "None"
        print $2
        descriptionNeeded = 1
      }
      END {
        if (descriptionNeeded)
          print "None"
      }' | sed 's/;$//')"

    echo oracleInstalledProducts="$(awk -F\" '{ORS=","} /product-family/ { print $2 }' $ORACLE_HOME/inventory/registry.xml | sed 's/,$//')"
fi

if [ -f "/etc/os-release" ]; then
  grep '=' /etc/os-release | sed 's/^/__OS__/'
fi
