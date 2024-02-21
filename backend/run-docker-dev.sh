#!/usr/bin/env bash
set -e
cd $(dirname $0)

./build-docker-image.sh
csk=cookie-secret-key.txt
if [ ! -f $csk ]; then
  head -c 16 /dev/urandom | openssl base64 > $csk
fi

docker run \
  -e "XXXX_DB_HOST=localhost" \
  -e "XXXX_DB_PASSWORD=xxxx" \
  -e "XXXX_DB_DATABASE_NAME=xxxx_dev" \
  -e "XXXX_SERVICE=registry" \
  -e "XXXX_AUTHENTICATION_COOKIE_SECRET_KEY=$(cat $csk)" \
  --network="host" -it --rm xxxx/backend
