#!/bin/sh
#
#Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

rm -Rf src/test/resources/wdt/archive
mkdir -p src/test/resources/wdt/archive/wlsdeploy/applications
cd src/test/resources/wdt/simple-app
jar cvf ../archive/wlsdeploy/applications/simple-app.war *
cd ../archive
jar cvf ../archive.zip *
