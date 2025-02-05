#!/bin/bash
# Only use this script when releasing bug fixes (removes SNAPSHOT for version)
set -e
git checkout main

project_version=$(mvn help:evaluate -q -DforceStdout -D"expression=project.version")
echo Current POM version: ${project_version}

project_version_number_only=$(echo $project_version | sed -e 's/[^0-9][^0-9]*$//')
last_digit=$(echo $project_version_number_only | sed -e 's/[0-9]*\.//g')
next_digit=$(($last_digit+1))
new_version=$(echo $project_version | sed -e "s/[0-9][0-9]*\([^0-9]*\)$/$next_digit\1/")

echo New Version: ${new_version}

mvn versions:set -DremoveSnapshot -DgenerateBackupPoms=false
mvn clean install
git add .
git commit -m "release ${project_version_number_only}"
git push

git tag release-${project_version_number_only}
git push origin release-${project_version_number_only}

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${new_version}
mvn clean install
git add .
git commit -m "preparing for next development iteration"
git push

