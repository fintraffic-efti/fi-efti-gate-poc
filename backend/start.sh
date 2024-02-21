#!/usr/bin/env bash

echo 'Start xxxx backend'
exec clojure \
  -J-Djava.awt.headless=true \
  -M --report stderr \
  -m xxxxx.xxxx.backend.system
