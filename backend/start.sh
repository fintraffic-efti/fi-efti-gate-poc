#!/usr/bin/env bash
cd $(dirname $0)

echo 'Start efti backend'
exec clojure \
  -J-Djava.awt.headless=true \
  -M --report stderr \
  -m fintraffic.efti.backend.system
