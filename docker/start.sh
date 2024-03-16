#!/usr/bin/env bash
set -e
cd $(dirname $0)

chmod a+rx initdb
chmod a+r initdb/01-init.sql

./alb/create-certificates.sh

docker compose up --detach

# Wait naively for PostgreSQL to start
sleep 2

echo "Migrate database"
./flyway.sh migrate
