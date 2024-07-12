#!/usr/bin/env bash
set -e
cd $(dirname $0)

chmod a+rx initdb
chmod a+r initdb/01-init.sql

./alb/create-certificates.sh

./harmony/config.sh 'fi1'
./harmony/config.sh 'fi2'

echo "Generate XML classes"
cd ../backend/
clojure -T:build xjc
clojure -T:build classes
cd ../docker/


echo "Build gate image"
../backend/build-docker-image.sh

docker compose up --detach

# Wait naively for PostgreSQL to start
sleep 2

echo "Migrate database"
./flyway.sh migrate
