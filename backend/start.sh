#!/usr/bin/env bash
cd $(dirname $0)

echo 'Generate jaxb classes'
clojure -T:build xjc
clojure -T:build classes

echo 'Start efti backend'
exec clojure \
  -J-Djava.awt.headless=true \
  -M --report stderr \
  -m fintraffic.efti.backend.system
