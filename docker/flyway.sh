#!/usr/bin/env bash
set -e
cd $(dirname $0)

if [ -z "$1" ]
then
  echo "Command is missing!"
  echo "Usage: $0 [migrate or clean]"
  exit 1
fi

cd ../db

export EFTI_DB_PASSWORD="efti"
export EFTI_DB_GATEWAY_PASSWORD="efti"
export EFTI_DB_HOST="localhost"
export EFTI_DB_PORT="8432"

# Don't run test migrations to template db: efti_template
EFTI_DB_DATABASE_NAME="efti_template" ./db.sh $1

# Run test migrations to efti_dev
EFTI_DB_DATABASE_NAME="efti_dev" ./db.sh $1 test
