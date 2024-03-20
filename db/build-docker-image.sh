#!/usr/bin/env bash
set -e
cd "$(dirname $0)"

registry_name="${1%/}/"
repository_name='efti/db'

containername=${registry_name#/}$repository_name
git_sha=$(git rev-parse HEAD)

echo "Building image $containername:$git_sha"
docker build . --tag $containername:$git_sha --tag $containername:latest
