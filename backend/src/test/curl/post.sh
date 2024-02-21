#!/usr/bin/env bash
set -e
cd $(dirname $0)

local='http://localhost:8888'
dev='https://registry.dev.efti.cloud.fintraffic.fi/'

server=${!1}
file=${2-'./example-instances-e2br3-testing-files_EDQM_Revision/1a. EEA SUSAR-patient-death (EDQM_revision).xml'}
type=${3-'hum'}

curl -v -d "@$file" \
  -H "Content-Type: text/xml" \
  "$server/api/private/reports/$type/xml"
