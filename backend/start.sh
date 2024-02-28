#!/usr/bin/env bash

echo 'Start efti backend'
exec clojure \
  -J-Djava.awt.headless=true \
  -M --report stderr \
  -m fintraffic.efti.backend.system
