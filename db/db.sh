#!/usr/bin/env bash
set -e

if [ "$2" == 'test' ]
then
  alias='-M:test'
else
  alias='-M'
fi

echo "Start db $1 alias=$alias"
exec clojure \
  -J-Djava.awt.headless=true \
  $alias --report stderr \
  -m fintraffic.efti.db.flywaydb $1
