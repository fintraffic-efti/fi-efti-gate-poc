#!/usr/bin/env bash
set -e
cd $(dirname $0)

etc_dir="$1/etc"

if [ -d "$etc_dir" ]; then
  echo "Harmony ap $1 configuration already exists in $etc_dir."
  echo "Remove the directory if you want to initialize configuration."
  exit 0
fi

echo "Create harmony ap $1 configuration"
mkdir -p "$etc_dir"
cp "$1/certificates"/*.* "$etc_dir/."
sudo chown -R 999:999 "$etc_dir"

echo "You need to upload pmode manually."