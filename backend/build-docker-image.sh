#!/usr/bin/env bash
set -e
cd $(dirname $0)

echo "Generate XML classes"
clojure -T:build xjc
clojure -T:build classes

registry_name="${1%/}/"
repository_name='efti/backend'

containername=${registry_name#/}$repository_name
git_sha=$(git rev-parse HEAD)

echo $git_sha > ./src/main/resources/git-revision
TZ=Europe/Helsinki date --iso=seconds > ./src/main/resources/build-date

echo "Building image $containername:$git_sha"
docker build . --tag $containername:$git_sha --tag $containername:latest
