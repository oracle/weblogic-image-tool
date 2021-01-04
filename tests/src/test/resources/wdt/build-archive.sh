#!/bin/sh
#
#Copyright (c) 2018, 2021, Oracle and/or its affiliates.
#
#Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

MODULE_DIR=$PWD
ARCHIVE_DIR=target/wdt/archive
EXPLODED_DIR=$ARCHIVE_DIR/wlsdeploy/applications

rm -Rf $ARCHIVE_DIR
mkdir -p $EXPLODED_DIR

cd src/test/resources/wdt/simple-app
jar cvf $MODULE_DIR/$EXPLODED_DIR/simple-app.war *
cd $MODULE_DIR/$ARCHIVE_DIR
jar cvf ../archive.zip *
